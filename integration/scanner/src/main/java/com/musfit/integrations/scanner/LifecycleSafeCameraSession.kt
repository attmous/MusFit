package com.musfit.integrations.scanner

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

internal class LifecycleSafeCameraSession<Provider, UseCase, Camera>(
    private val providerFuture: ListenableFuture<Provider>,
    private val callbackExecutor: Executor,
    private val bindUseCases: (Provider, List<UseCase>) -> Camera,
    private val unbindUseCases: (Provider, List<UseCase>) -> Unit,
    private val onFailure: (Throwable) -> Unit,
) : AutoCloseable {
    private val active = AtomicBoolean(true)
    private val started = AtomicBoolean(false)
    private var provider: Provider? = null
    private var ownedUseCases: List<UseCase> = emptyList()

    fun start(useCases: List<UseCase>, onBound: (Camera) -> Unit) {
        check(started.compareAndSet(false, true)) { "Camera session already started." }
        try {
            providerFuture.addListener({
                if (!active.get()) return@addListener
                val acquiredProvider = try {
                    providerFuture.get()
                } catch (_: CancellationException) {
                    return@addListener
                } catch (error: ExecutionException) {
                    onFailure(error.cause ?: error)
                    return@addListener
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                    onFailure(error)
                    return@addListener
                }
                if (!active.get()) return@addListener
                try {
                    val camera = bindUseCases(acquiredProvider, useCases)
                    if (!active.get()) {
                        unbindUseCases(acquiredProvider, useCases)
                        return@addListener
                    }
                    provider = acquiredProvider
                    ownedUseCases = useCases
                    onBound(camera)
                } catch (error: IllegalArgumentException) {
                    if (active.get()) onFailure(error)
                } catch (error: IllegalStateException) {
                    if (active.get()) onFailure(error)
                }
            }, callbackExecutor)
        } catch (error: RejectedExecutionException) {
            if (active.get()) onFailure(error)
        }
    }

    fun unbindOwned() {
        val acquiredProvider = provider ?: return
        val useCases = ownedUseCases
        provider = null
        ownedUseCases = emptyList()
        if (useCases.isNotEmpty()) unbindUseCases(acquiredProvider, useCases)
    }

    override fun close() {
        if (!active.compareAndSet(true, false)) return
        if (!providerFuture.isDone) providerFuture.cancel(true)
        unbindOwned()
    }
}

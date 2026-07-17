package com.musfit.integrations.scanner

import com.google.common.util.concurrent.SettableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executor

class CameraSessionOwnerTest {
    private val directExecutor = Executor { command -> command.run() }

    @Test
    fun closeBeforeProviderCompletes_cancelsFutureAndNeverBinds() {
        val provider = SettableFuture.create<String>()
        val bound = mutableListOf<List<String>>()
        val session = session(provider, bound = bound)

        session.start(listOf("preview", "analysis")) { }
        session.close()

        assertTrue(provider.isCancelled)
        assertEquals(emptyList<List<String>>(), bound)
    }

    @Test
    fun closeAfterBinding_unbindsOnlyOwnedUseCasesExactlyOnce() {
        val provider = SettableFuture.create<String>().apply { set("provider") }
        val bound = mutableListOf<List<String>>()
        val unbound = mutableListOf<List<String>>()
        val cameras = mutableListOf<String>()
        val session = session(provider, bound = bound, unbound = unbound)

        session.start(listOf("preview", "analysis"), cameras::add)
        session.close()
        session.close()

        assertEquals(listOf(listOf("preview", "analysis")), bound)
        assertEquals(listOf(listOf("preview", "analysis")), unbound)
        assertEquals(listOf("camera"), cameras)
    }

    @Test
    fun providerFailure_isReportedWithoutBinding() {
        val provider = SettableFuture.create<String>().apply { setException(IOException("camera unavailable")) }
        val errors = mutableListOf<Throwable>()
        val bound = mutableListOf<List<String>>()
        val session = session(provider, bound = bound, errors = errors)

        session.start(listOf("preview", "analysis")) { }

        assertEquals(emptyList<List<String>>(), bound)
        assertEquals("camera unavailable", errors.single().message)
    }

    @Test
    fun startTwiceBeforeProviderCompletes_isRejected() {
        val provider = SettableFuture.create<String>()
        val session = session(provider, bound = mutableListOf())

        session.start(listOf("preview", "analysis")) { }

        assertThrows(IllegalStateException::class.java) {
            session.start(listOf("second-preview")) { }
        }
        session.close()
    }

    @Test
    fun twentyRapidDelayedProviderSessions_cancelWithoutBinding() {
        val bound = mutableListOf<List<String>>()

        repeat(20) {
            val provider = SettableFuture.create<String>()
            val session = session(provider, bound = bound)
            session.start(listOf("preview-$it", "analysis-$it")) { }
            session.close()
            assertTrue(provider.isCancelled)
        }

        assertTrue(bound.isEmpty())
    }

    private fun session(
        provider: SettableFuture<String>,
        bound: MutableList<List<String>>,
        unbound: MutableList<List<String>> = mutableListOf(),
        errors: MutableList<Throwable> = mutableListOf(),
    ) = LifecycleSafeCameraSession<String, String, String>(
        providerFuture = provider,
        callbackExecutor = directExecutor,
        bindUseCases = { _, useCases ->
            bound.add(useCases)
            "camera"
        },
        unbindUseCases = { _, useCases -> unbound.add(useCases) },
        onFailure = errors::add,
    )
}

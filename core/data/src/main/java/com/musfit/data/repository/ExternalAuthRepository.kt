package com.musfit.data.repository

import com.musfit.data.remote.auth.GitHubAuthApi
import com.musfit.data.remote.auth.GitHubDeviceCodeResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject

data class GitHubDeviceAuthorization(
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
)

interface ExternalAuthRepository {
    val isGitHubConfigured: Boolean
    suspend fun signInWithGitHub(
        onDeviceAuthorization: suspend (GitHubDeviceAuthorization) -> Unit,
    ): ExternalAccountProfile
}

class GitHubExternalAuthRepository @Inject constructor(
    private val authConfig: AuthConfig,
    private val gitHubAuthApi: GitHubAuthApi,
) : ExternalAuthRepository {
    override val isGitHubConfigured: Boolean
        get() = authConfig.isGitHubConfigured

    override suspend fun signInWithGitHub(
        onDeviceAuthorization: suspend (GitHubDeviceAuthorization) -> Unit,
    ): ExternalAccountProfile {
        if (!authConfig.isGitHubConfigured) {
            throw IllegalStateException("GitHub sign-in is not configured.")
        }

        val deviceCode = gitHubAuthApi.requestDeviceCode(
            clientId = authConfig.githubOAuthClientId,
            scope = GITHUB_SCOPE,
        )
        onDeviceAuthorization(deviceCode.toAuthorization())

        val accessToken = pollForAccessToken(deviceCode)
        return fetchGitHubProfile(accessToken)
    }

    private suspend fun pollForAccessToken(deviceCode: GitHubDeviceCodeResponse): String {
        var pollIntervalSeconds = deviceCode.intervalSeconds.coerceAtLeast(MIN_POLL_INTERVAL_SECONDS)
        val deadline = System.currentTimeMillis() + deviceCode.expiresInSeconds * 1_000L
        while (System.currentTimeMillis() < deadline) {
            delay(pollIntervalSeconds * 1_000L)
            val tokenResponse = gitHubAuthApi.requestAccessToken(
                clientId = authConfig.githubOAuthClientId,
                deviceCode = deviceCode.deviceCode,
                grantType = DEVICE_GRANT_TYPE,
            )
            val accessToken = tokenResponse.accessToken?.takeIf { it.isNotBlank() }
            if (accessToken != null) return accessToken

            when (tokenResponse.error) {
                "authorization_pending" -> Unit

                "slow_down" -> pollIntervalSeconds += SLOW_DOWN_INCREMENT_SECONDS

                "expired_token" -> throw IllegalStateException("GitHub sign-in code expired.")

                else -> throw IllegalStateException(
                    tokenResponse.errorDescription ?: "GitHub sign-in failed.",
                )
            }
        }
        throw IllegalStateException("GitHub sign-in code expired.")
    }

    private suspend fun fetchGitHubProfile(accessToken: String): ExternalAccountProfile {
        val authorization = "Bearer $accessToken"
        val user = gitHubAuthApi.getUser(authorization)
        val primaryEmail = try {
            gitHubAuthApi.getEmails(authorization)
                .firstOrNull { it.primary && it.verified }
                ?.email
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        return ExternalAccountProfile(
            provider = AccountAuthProvider.GitHub,
            providerUserId = user.id.toString(),
            displayName = user.name?.takeIf { it.isNotBlank() } ?: user.login,
            email = primaryEmail ?: user.email,
            avatarUrl = user.avatarUrl,
        )
    }

    private companion object {
        const val GITHUB_SCOPE = "read:user user:email"
        const val DEVICE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
        const val MIN_POLL_INTERVAL_SECONDS = 5
        const val SLOW_DOWN_INCREMENT_SECONDS = 5
    }
}

private fun GitHubDeviceCodeResponse.toAuthorization(): GitHubDeviceAuthorization = GitHubDeviceAuthorization(
    userCode = userCode,
    verificationUri = verificationUri,
    expiresInSeconds = expiresInSeconds,
)

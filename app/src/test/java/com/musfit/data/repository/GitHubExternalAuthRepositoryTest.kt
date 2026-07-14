package com.musfit.data.repository

import com.musfit.data.remote.auth.GitHubAccessTokenResponse
import com.musfit.data.remote.auth.GitHubAuthApi
import com.musfit.data.remote.auth.GitHubDeviceCodeResponse
import com.musfit.data.remote.auth.GitHubEmailResponse
import com.musfit.data.remote.auth.GitHubUserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test

class GitHubExternalAuthRepositoryTest {
    @Test
    fun signInWithGitHub_emailCancellationPropagatesWithoutReturningLinkableProfile() = runTest {
        val repository = GitHubExternalAuthRepository(
            authConfig = AuthConfig(googleWebClientId = "", githubOAuthClientId = "client-id"),
            gitHubAuthApi = CancelingEmailGitHubAuthApi(),
        )

        try {
            repository.signInWithGitHub { }
            fail("Expected cancellation instead of a profile")
        } catch (_: CancellationException) {
            // Expected: callers cannot continue into account linking.
        }
    }
}

private class CancelingEmailGitHubAuthApi : GitHubAuthApi {
    override suspend fun requestDeviceCode(clientId: String, scope: String) = GitHubDeviceCodeResponse(
        deviceCode = "device-code",
        userCode = "user-code",
        verificationUri = "https://github.com/login/device",
        expiresInSeconds = 900,
        intervalSeconds = 0,
    )

    override suspend fun requestAccessToken(
        clientId: String,
        deviceCode: String,
        grantType: String,
    ) = GitHubAccessTokenResponse(accessToken = "access-token")

    override suspend fun getUser(authorization: String) = GitHubUserResponse(
        id = 42L,
        login = "octocat",
    )

    override suspend fun getEmails(authorization: String): List<GitHubEmailResponse> = throw CancellationException("Sign-in screen closed")
}

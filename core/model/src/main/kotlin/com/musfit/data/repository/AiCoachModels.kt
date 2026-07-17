package com.musfit.data.repository

enum class AiCoachProviderKind {
    Disabled,
    OpenAiCompatible,
    LocalAgent,
}

enum class LocalAgentKind {
    OpenClaw,
    HermesAgent,
    Custom,
}

data class AiCoachConnection(
    val providerKind: AiCoachProviderKind,
    val baseUrl: String,
    val modelName: String,
    val localAgentKind: LocalAgentKind,
    val apiKey: String?,
)

data class AuthConfig(
    val googleWebClientId: String,
    val githubOAuthClientId: String,
) {
    val isGoogleConfigured: Boolean = googleWebClientId.isNotBlank()
    val isGitHubConfigured: Boolean = githubOAuthClientId.isNotBlank()
}

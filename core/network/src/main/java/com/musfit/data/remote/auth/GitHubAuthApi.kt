package com.musfit.data.remote.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface GitHubAuthApi {
    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String,
    ): GitHubDeviceCodeResponse

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/oauth/access_token")
    suspend fun requestAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String,
    ): GitHubAccessTokenResponse

    @Headers("Accept: application/vnd.github+json")
    @GET("https://api.github.com/user")
    suspend fun getUser(
        @Header("Authorization") authorization: String,
    ): GitHubUserResponse

    @Headers("Accept: application/vnd.github+json")
    @GET("https://api.github.com/user/emails")
    suspend fun getEmails(
        @Header("Authorization") authorization: String,
    ): List<GitHubEmailResponse>
}

@JsonClass(generateAdapter = false)
data class GitHubDeviceCodeResponse(
    @param:Json(name = "device_code") val deviceCode: String,
    @param:Json(name = "user_code") val userCode: String,
    @param:Json(name = "verification_uri") val verificationUri: String,
    @param:Json(name = "expires_in") val expiresInSeconds: Int,
    @param:Json(name = "interval") val intervalSeconds: Int = 5,
)

@JsonClass(generateAdapter = false)
data class GitHubAccessTokenResponse(
    @param:Json(name = "access_token") val accessToken: String? = null,
    @param:Json(name = "token_type") val tokenType: String? = null,
    @param:Json(name = "error") val error: String? = null,
    @param:Json(name = "error_description") val errorDescription: String? = null,
)

@JsonClass(generateAdapter = false)
data class GitHubUserResponse(
    val id: Long,
    val login: String,
    val name: String? = null,
    val email: String? = null,
    @param:Json(name = "avatar_url") val avatarUrl: String? = null,
)

@JsonClass(generateAdapter = false)
data class GitHubEmailResponse(
    val email: String,
    val primary: Boolean = false,
    val verified: Boolean = false,
)

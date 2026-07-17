package com.musfit.ui.profile

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.musfit.data.repository.AccountAuthProvider
import com.musfit.data.repository.ExternalAccountProfile

internal suspend fun signInWithGoogle(
    context: Context,
    googleWebClientId: String,
): ExternalAccountProfile {
    if (googleWebClientId.isBlank()) {
        throw IllegalStateException("Google sign-in is not configured.")
    }
    val googleOption = GetSignInWithGoogleOption.Builder(googleWebClientId).build()
    val request =
        GetCredentialRequest
            .Builder()
            .addCredentialOption(googleOption)
            .build()

    val credential =
        try {
            CredentialManager
                .create(context)
                .getCredential(context = context, request = request)
                .credential
        } catch (error: NoCredentialException) {
            throw IllegalStateException("No Google account is available for sign-in.", error)
        }

    if (credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        throw IllegalStateException("Google sign-in did not return a Google account.")
    }

    val googleAccount =
        try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (error: GoogleIdTokenParsingException) {
            throw IllegalStateException("Google sign-in returned an unreadable credential.", error)
        }
    val email = googleAccount.email?.takeIf { it.isNotBlank() }
    val providerUserId =
        googleAccount.uniqueId?.takeIf { it.isNotBlank() }
            ?: email
            ?: googleAccount.id.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google sign-in did not return an account id.")
    val displayName =
        googleAccount.displayName?.takeIf { it.isNotBlank() }
            ?: email
            ?: "Google user"
    return ExternalAccountProfile(
        provider = AccountAuthProvider.Google,
        providerUserId = providerUserId,
        displayName = displayName,
        email = email,
        avatarUrl = googleAccount.profilePictureUri?.toString(),
    )
}

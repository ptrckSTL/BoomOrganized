package com.simplemobiletools.boomorganized.oauth

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.gson.annotations.SerializedName
import com.simplemobiletools.boomorganized.requestVariousScopes
import com.simplemobiletools.smsmessenger.activities.MainActivity
import kotlinx.coroutines.CoroutineExceptionHandler

data class OauthClient(
    @SerializedName("installed")
    val installed: Installed,
) {
    data class Installed(
        @SerializedName("auth_provider_x509_cert_url")
        val authProviderX509CertUrl: String,
        @SerializedName("auth_uri")
        val authUri: String,
        @SerializedName("client_id")
        val clientId: String,
        @SerializedName("project_id")
        val projectId: String,
        @SerializedName("token_uri")
        val tokenUri: String,
    )

    companion object {
        val requiredScopes = listOf(Scope(SheetsScopes.SPREADSHEETS), Scope(Scopes.DRIVE_FULL))
    }
}

fun Activity.oauthClient(): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestVariousScopes(OauthClient.requiredScopes)
        .build()
    return GoogleSignIn.getClient(this, gso)
}


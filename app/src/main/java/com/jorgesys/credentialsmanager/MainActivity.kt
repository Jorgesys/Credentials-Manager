package com.jorgesys.credentialsmanager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.jorgesys.credentialsmanager.ui.theme.CredentialsManagerTheme

import androidx.credentials.GetCredentialRequest.Builder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/*
Documentación
https://developer.android.com/identity/sign-in/credential-manager-siwg

"API Project"
https://console.cloud.google.com/apis/credentials?project=api-project-284968082006

OAuth Android Credential
Client ID: 284968082006-gotl1bjvn243fh2ud9cf0ohv28plsdpt.apps.googleusercontent.com

OAuth Web Credential
Client ID: 284968082006-j5pfudbn91faprqi7fm8rov7o1uleus0.apps.googleusercontent.com
Client secret: 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
Creation date: December 27, 2024 at 9:55:20 AM GMT-

SHA1 (Jorge): 56:48:1F:75:26:E6:E2:D7:25:85:80:70:97:79:0A:5F:78:F7:51:7B

 */

class MainActivity : ComponentActivity() {

    val TAG = "CredentialsManager"

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //***Instantiate a Google sign-in request
        var WEB_CLIENT_ID = "284968082006-j5pfudbn91faprqi7fm8rov7o1uleus0.apps.googleusercontent.com" //Android Id
        //https://developer.android.com/google/play/integrity/classic#nonce
        //"from a cryptographically generated random number on the server side or from a pre-existing identifier, such as a session or transaction ID."
        //To improve sign-in security and avoid replay attacks, add setNonce to include a nonce in each request.
        var nonceString = "123abcDEF456" //nonce string to use when generating a Google ID token
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(WEB_CLIENT_ID)
            //.setAutoSelectEnabled(true) //Enable automatic sign-in for returning users (recommended)
            //.setNonce(nonceString)
            .build()
        Log.i(TAG, "googleIdOption serverClientId:${googleIdOption.serverClientId}|linkedServiceId:${googleIdOption.linkedServiceId}|nonce:${googleIdOption.nonce}|autoSelectEnabled:${googleIdOption.autoSelectEnabled}|requestVerifiedPhoneNumber:${googleIdOption.requestVerifiedPhoneNumber}")

        //***Create the Sign in with Google flow
        val request: GetCredentialRequest = Builder()
            .addCredentialOption(googleIdOption)
            .build()
        Log.i(TAG, "request ${request.credentialOptions}")

        val credentialManager = CredentialManager.create(this)

        var activityContext = this
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext,
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                handleFailure(e)
            }
        }

        setContent {
            CredentialsManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

    }

    fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential
        Log.i(TAG, "handleSignIn() ${credential.data.also { it.keySet() } ?: "N/A"}")

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        Log.i(TAG, "handleSignIn() ${googleIdTokenCredential.id}|${googleIdTokenCredential.idToken}|${googleIdTokenCredential.displayName}")
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "handleSignIn() Received an invalid google id token response", e)
                    }
                } else {
                    // Catch any unrecognized credential type here.
                    Log.e(TAG, "handleSignIn()  Unexpected type of credential: ${credential.type}")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential, credential.type:${credential.type}!")
            }
        }
    }

    fun handleFailure(e: Exception) {
        Log.e(TAG, "handleFailure() :  ${e.message}")
    }

    fun handleSignIn2(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential
        var responseJson : String

        when (credential) {

            // Passkey credential
            is PublicKeyCredential -> {
                // Share responseJson such as a GetCredentialResponse on your server to
                // validate and authenticate
                responseJson = credential.authenticationResponseJson
                Log.i(TAG, "handleSignIn() responseJson:${responseJson}")
            }

            // Password credential
            is PasswordCredential -> {
                // Send ID and password to your server to validate and authenticate.
                val username = credential.id
                val password = credential.password
                Log.i(TAG, "handleSignIn() PasswordCredential-> username:${username}|password:${password}")
            }

            // GoogleIdToken credential
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract the ID to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        Log.i(TAG, "handleSignIn() ${googleIdTokenCredential.id}|${googleIdTokenCredential.idToken}|${googleIdTokenCredential.displayName}")
                        // You can use the members of googleIdTokenCredential directly for UX
                        // purposes, but don't use them to store or control access to user
                        // data. For that you first need to validate the token:
                        // pass googleIdTokenCredential.getIdToken() to the backend server.

                        //TODO:JOC PENDING GoogleIdTokenVerifier verifier = ... // see validation instructions
                        //TODO:JOC PENDINGGoogleIdToken idToken = verifier.verify(idTokenString);

                        // To get a stable account identifier (e.g. for storing user data),
                        // use the subject ID:

                        //TODO:JOC PENDINGidToken.getPayload().getSubject()

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Credentials Manager: $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CredentialsManagerTheme {
        Greeting("Android")
    }
}


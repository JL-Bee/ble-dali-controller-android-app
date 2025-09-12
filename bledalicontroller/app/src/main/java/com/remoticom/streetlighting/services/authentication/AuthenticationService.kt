package com.remoticom.streetlighting.services.authentication

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import com.remoticom.streetlighting.BuildConfig
import com.remoticom.streetlighting.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthenticationService {

  // Statuses
  enum class Status {
    UNAUTHENTICATED,        // Initial state, the user needs to authenticate
    AUTHENTICATING,
    AUTHENTICATED,        // The user has authenticated successfully
  }

  // State
  data class State(
    val status: Status = Status.UNAUTHENTICATED,
    val lastError: Error? = null,
    val account: IAccount? = null
  )

  private val _state = MutableLiveData(State())
  val state: LiveData<State> = _state

  private var serviceState = State()
    set(value) {
      Log.d(TAG, "Service state updated with $value")
      field = value
      _state.postValue(value)
    }

  // Access Token
  var accessToken: String? = null
    private set

  //
  private val scopes = arrayOf("https://remoticomb2c.onmicrosoft.com/api/User.Read")
  private val authority = "https://remoticomb2c.b2clogin.com/tfp/remoticomb2c.onmicrosoft.com/b2c_1_signup_signin/"

  private var singleAccountApplication: ISingleAccountPublicClientApplication? = null;

  fun start(activity: Activity) {
    Log.d(TAG, "Creating single account public client app...")

    updateStatus(Status.AUTHENTICATING)

    startInternal(activity, object : AuthenticationStatusCallback {
      override fun onAuthenticationSucceeded() {
        updateStatus(Status.AUTHENTICATED)
      }

      override fun onAuthenticationFailed() {
        updateStatus(Status.UNAUTHENTICATED)
      }
    })
  }

  interface AuthenticationStatusCallback {
    fun onAuthenticationSucceeded()
    fun onAuthenticationFailed()
  }

  private fun startInternal(activity: Activity, callback: AuthenticationStatusCallback) {
    if (BuildConfig.DEBUG) {
      Logger.getInstance().setEnableLogcatLog(true)
      // Logger.getInstance().setEnablePII(true)
      Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE)
    }

    PublicClientApplication.createSingleAccountPublicClientApplication(activity.applicationContext, R.raw.auth_config_b2c, object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
      override fun onCreated(application: ISingleAccountPublicClientApplication?) {
        Log.d(TAG, "Single account public client app created")

        singleAccountApplication = application

        acquireTokenInternal(activity, callback)
      }

      override fun onError(exception: MsalException?) {
        Log.e(TAG, "Error creating single account public client app: ${exception?.message} (${exception?.errorCode})")

        serviceState = serviceState.copy(lastError = Error(exception))
      }
    })
  }

  fun login(activity: Activity) {
    Log.d(TAG, "Signing in...")

    updateStatus(Status.AUTHENTICATING)

    singleAccountApplication?.signIn(activity, "", scopes, object :
      AuthenticationCallback {
      override fun onSuccess(authenticationResult: IAuthenticationResult?) {
        Log.i(TAG, "Sign in successful")

        handleAuthenticationSuccess(authenticationResult)

        updateStatus(Status.AUTHENTICATED)
      }

      override fun onError(exception: MsalException?) {
        Log.i(TAG, "Sign in error ${exception?.message} (${exception?.errorCode})")

        handleAuthenticationError(exception)

        updateStatus(Status.UNAUTHENTICATED)
      }

      override fun onCancel() {
        Log.i(TAG, "Sign in cancelled")

        handleAuthenticationCancel()

        updateStatus(Status.UNAUTHENTICATED)
      }
    })
  }

  fun logout() {
    Log.d(TAG, "Signing out...")

    singleAccountApplication?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
      override fun onSignOut() {
        Log.i(TAG, "Sign out successful")

        accessToken = null
        serviceState = serviceState.copy(status = Status.UNAUTHENTICATED, account = null)
      }

      override fun onError(exception: MsalException) {
        Log.e(TAG, "Sign out error: ${exception.message} (${exception.errorCode})")

        serviceState = serviceState.copy(lastError = Error(exception))
      }
    })
  }

  suspend fun refreshToken() : Unit =
    suspendCoroutine { cont ->
      acquireTokenInternal(null, callback = object : AuthenticationStatusCallback {
        override fun onAuthenticationFailed() {
          logout()

          cont.resume(Unit)
        }

        override fun onAuthenticationSucceeded() {
          cont.resume(Unit)
        }
      })
    }


  private fun acquireTokenInternal(activity: Activity?, callback: AuthenticationStatusCallback) {
    loadAccount(object : LoadAccountCallback {
      override fun onAccountLoaded(account: IAccount?) {
        Log.d(TAG, "Acquiring token...")

        if (null == account) {
          callback.onAuthenticationFailed()

          return
        }

        singleAccountApplication?.acquireTokenSilentAsync(scopes, authority, object: SilentAuthenticationCallback {
          override fun onSuccess(authenticationResult: IAuthenticationResult?) {
            Log.i(TAG, "Token acquired silently. Token expires on ${authenticationResult?.expiresOn}")

            handleAuthenticationSuccess(authenticationResult)

            callback.onAuthenticationSucceeded()
          }

          override fun onError(exception: MsalException?) {
            Log.w(TAG,"Error acquiring token: ${exception?.message} (${exception?.errorCode})")

            if (exception is MsalClientException) {
              Log.e(TAG, "Cannot recover. Logging out...")

              logout()

              return
            }

            if (exception is MsalUiRequiredException) {
              if (null == activity) {
                Log.d(TAG, "Cannot acquire silently. Logging out...")

                logout()

                return

              } else {
                Log.d(TAG, "Cannot acquire silently. Acquiring interactive...")

                singleAccountApplication?.acquireToken(activity, scopes, object : AuthenticationCallback {
                  override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                    Log.i(TAG, "Token acquired interactively")

                    handleAuthenticationSuccess(authenticationResult)

                    callback.onAuthenticationSucceeded()
                  }

                  override fun onError(exception: MsalException?) {
                    Log.e(TAG, "Error acquiring token interactively: ${exception?.message} (${exception?.errorCode})")

                    handleAuthenticationError(exception)

                    callback.onAuthenticationFailed()
                  }

                  override fun onCancel() {
                    Log.i(TAG, "Acquiring token interactively cancelled")

                    handleAuthenticationCancel()

                    callback.onAuthenticationFailed()
                  }
                })
              }
            }
          }
        })
      }
    })
  }

  // To be called from onResume()
  fun resume() {
    // The account may have been removed from the device (if broker is in use).
    // Therefore, we want to update the account state by invoking loadAccount() here.
    // Source: https://github.com/Azure-Samples/ms-identity-android-kotlin/blob/master/app/src/main/java/com/azuresamples/msalandroidkotlinapp/SingleAccountModeFragment.kt

    if (serviceState.status != Status.AUTHENTICATING) {
      loadAccount()
    }
  }

  private fun updateStatus(newStatus: Status) {
    serviceState = serviceState.copy(status = newStatus)
  }

  private fun handleAuthenticationSuccess(authenticationResult: IAuthenticationResult?) {
    authenticationResult?.let {
      accessToken = it.accessToken
      serviceState = serviceState.copy(status = Status.AUTHENTICATED, account = it.account)

      Log.d(TAG, "Access Token: ${accessToken}")
    }

    // loadAccount()
  }

  private fun handleAuthenticationError(exception: MsalException?) {
    exception?.let {
      Log.e(TAG, "Authentication error: ${it.message} (${it.errorCode})")
    }

    if (exception is MsalClientException) {
      // Exception inside MSAL, more info inside MsalError.java
    } else if (exception is MsalServiceException) {
      // Exception when communicating with the STS, likely config issue
    } else if (exception is MsalUiRequiredException) {
      // Tokens expired or no session, retry with interactive
    }

    accessToken = null

    serviceState = serviceState.copy(
      lastError = Error(exception),
      account = null
    )
  }

  private fun handleAuthenticationCancel() {
    // No change?!
  }

  interface LoadAccountCallback {
    fun onAccountLoaded(account: IAccount?)
  }

  private fun loadAccount(callback: LoadAccountCallback? = null) {
    Log.d(TAG, "Loading account...")

    singleAccountApplication?.let { it ->
      it.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
        override fun onAccountLoaded(activeAccount: IAccount?) {
          Log.i(TAG, "Account loaded for: ${activeAccount?.claims?.get("name")}")

          serviceState = serviceState.copy(account = activeAccount)

          callback?.let { cb ->
            cb.onAccountLoaded(activeAccount)
          }
        }

        override fun onAccountChanged(
          priorAccount: IAccount?,
          currentAccount: IAccount?
        ) {
          Log.i(TAG, "Account changed: from ${priorAccount?.username} to ${currentAccount?.username}")

          if (null == currentAccount) {
            Log.w(TAG, "Signed-in account gone")
          }
        }

        override fun onError(exception: MsalException) {
          Log.e(TAG, "Error loading account: ${exception.message} (${exception.errorCode})")

          serviceState = serviceState.copy(lastError = Error(exception))
        }
      })
    }
  }


  companion object {
    private const val TAG = "AuthenticationService"

    @Volatile
    private var instance: AuthenticationService? = null

    fun getInstance(
    ) =
      instance
        ?: synchronized(this) {
          instance
            ?: AuthenticationService().also {
              instance = it
            }
        }
  }
}

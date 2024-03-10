package com.arn.scrobble.onboarding

import android.os.Bundle
import androidx.navigation.NavController
import com.arn.scrobble.App
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle

class LoginFlows(private val navController: NavController) {

    private fun lastfm() {
        val arguments = Bundle().apply {
            putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
            putBoolean(Stuff.ARG_SAVE_COOKIES, true)
            putSingle(
                UserAccountTemp(
                    AccountType.LASTFM,
                    "",
                )
            )
        }
        navController.navigate(R.id.webViewFragment, arguments)
    }

    private fun librefm() {
        val arguments = Bundle().apply {
            putString(Stuff.ARG_URL, Stuff.LIBREFM_AUTH_CB_URL)
            putSingle(
                UserAccountTemp(
                    AccountType.LIBREFM,
                    "",
                    Stuff.LIBREFM_API_ROOT
                )
            )
        }
        navController.navigate(R.id.webViewFragment, arguments)
    }

    private fun gnufm() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.gnufm),
            App.context.getString(R.string.password),
        ).apply {
            textField1 = App.context.getString(R.string.api_url)
            textField2 = App.context.getString(R.string.username)
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun listenbrainz() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.listenbrainz),
            App.context.getString(R.string.pref_token_label),
        ).apply {
            infoText = App.context.getString(
                R.string.listenbrainz_info,
                "https://listenbrainz.org/profile"
            )
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun customListenbrainz() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.custom_listenbrainz),
            App.context.getString(R.string.pref_token_label),
        ).apply {
            textField1 = App.context.getString(R.string.api_url)
            infoText = App.context.getString(
                R.string.listenbrainz_info,
                "[API_URL]/profile"
            )
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun maloja() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.maloja),
            App.context.getString(R.string.pref_token_label),
        ).apply {
            textField1 = App.context.getString(R.string.server_url)
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun pleroma() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.pleroma),
            App.context.getString(R.string.server_url),
        )
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }

    private fun scrobbleToFile() {
        navController.navigate(R.id.loginScrobbleToFile)
    }

    fun acrCloud() {
        val arguments = LoginFragmentArgs.Builder(
            App.context.getString(R.string.add_acr_key),
            App.context.getString(R.string.acr_secret)
        ).apply {
            infoText = App.context.getString(R.string.add_acr_key_info)
            textField1 = App.context.getString(R.string.acr_host)
            textField2 = App.context.getString(R.string.acr_key)
        }
            .build()
            .toBundle()
        navController.navigate(R.id.loginFragment, arguments)
    }


    fun go(accountType: AccountType) {
        when (accountType) {
            AccountType.LASTFM -> lastfm()
            AccountType.LIBREFM -> librefm()
            AccountType.GNUFM -> gnufm()
            AccountType.LISTENBRAINZ -> listenbrainz()
            AccountType.CUSTOM_LISTENBRAINZ -> customListenbrainz()
            AccountType.MALOJA -> maloja()
            AccountType.PLEROMA -> pleroma()
            AccountType.FILE -> scrobbleToFile()
        }
    }
}
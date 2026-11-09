package com.mthtamim.smartdialer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast

/**
 * Places the call for one provider.
 *
 * Honest limit, also written in the README: Android cannot place a call
 * inside another company's IP app. Tapping an IP provider only opens
 * that app with the number already filled in (ACTION_DIAL with the
 * provider package), so the user presses call once inside that app.
 * Only the Original SIM route can dial straight from Smart Dialer.
 */
object CallHelper {

    private const val TAG = "SmartDialer"

    data class CallResult(
        val started: Boolean,
        val message: String
    )

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (e: Exception) {
            Log.w(TAG, "isInstalled check failed for " + packageName, e)
            false
        }
    }

    fun placeCall(
        context: Context,
        rawNumber: String,
        provider: Provider,
        autoCallOn: Boolean,
        simSlot: Int,
        contactName: String
    ): CallResult {
        val number = Store.normalize(rawNumber)
        if (number.isEmpty()) {
            return CallResult(false, "Enter a number first")
        }
        val result: CallResult = if (provider.isSim) {
            placeSimCall(context, number, autoCallOn, simSlot)
        } else {
            placeIpCall(context, number, provider)
        }
        if (result.started) {
            Store.addRecent(
                context,
                RecentEntry(
                    number = number,
                    name = contactName,
                    providerId = provider.id,
                    timeMillis = System.currentTimeMillis(),
                    durationSec = 0L
                )
            )
        }
        return result
    }

    private fun placeSimCall(
        context: Context,
        number: String,
        autoCallOn: Boolean,
        simSlot: Int
    ): CallResult {
        val uri = Uri.parse("tel:" + number)
        if (!autoCallOn) {
            return try {
                val dial = Intent(Intent.ACTION_DIAL, uri)
                dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(dial)
                CallResult(true, "Dialer opened")
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No dialer activity", e)
                CallResult(false, "Could not open dialer")
            }
        }
        // Auto-call ON: try the chosen SIM first, then fall back to a
        // plain direct call. Both need CALL_PHONE; without it the
        // system dialer is the only safe step.
        val handle = phoneAccountForSlot(context, simSlot)
        if (handle != null) {
            try {
                val telecom = context.getSystemService(Context.TELECOM_SERVICE)
                    as TelecomManager
                val extras = Bundle()
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                telecom.placeCall(uri, extras)
                return CallResult(true, "Calling...")
            } catch (e: SecurityException) {
                Log.w(TAG, "placeCall needs CALL_PHONE", e)
            } catch (e: Exception) {
                Log.w(TAG, "placeCall failed, falling back to ACTION_CALL", e)
            }
        }
        return try {
            val call = Intent(Intent.ACTION_CALL, uri)
            call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(call)
            CallResult(true, "Calling...")
        } catch (e: SecurityException) {
            Log.w(TAG, "ACTION_CALL needs CALL_PHONE", e)
            try {
                val dial = Intent(Intent.ACTION_DIAL, uri)
                dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(dial)
                CallResult(true, "No call permission, dialer opened")
            } catch (e2: ActivityNotFoundException) {
                Log.w(TAG, "No dialer activity", e2)
                CallResult(false, "Could not open dialer")
            }
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No dialer activity", e)
            CallResult(false, "Could not open dialer")
        }
    }

    /** Best-effort SIM pick: the system decides the order of its
     * call-capable accounts. Slot 0 is normally SIM 1, slot 1 SIM 2. */
    private fun phoneAccountForSlot(
        context: Context,
        simSlot: Int
    ): android.telecom.PhoneAccountHandle? {
        return try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE)
                as TelecomManager
            val accounts = telecom.callCapablePhoneAccounts
            if (accounts == null || accounts.isEmpty()) {
                null
            } else if (simSlot < accounts.size) {
                accounts[simSlot]
            } else {
                accounts[0]
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Phone accounts need READ_PHONE_STATE", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Phone accounts lookup failed", e)
            null
        }
    }

    private fun placeIpCall(
        context: Context,
        number: String,
        provider: Provider
    ): CallResult {
        val pkg = provider.packageName
        if (pkg == null) {
            // No verified package: the only honest step is a Play
            // Store search for the app name.
            openPlaySearch(context, provider.playSearchQuery)
            return CallResult(
                false,
                provider.name + " app package not verified, Play Store search opened"
            )
        }
        if (!isInstalled(context, pkg)) {
            openPlayDetails(context, pkg)
            return CallResult(
                false,
                provider.name + " not installed, Play Store opened"
            )
        }
        val uri = Uri.parse("tel:" + number)
        return try {
            val dial = Intent(Intent.ACTION_DIAL, uri)
            dial.setPackage(pkg)
            dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(dial)
            CallResult(true, provider.name + " opened, press Call")
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "IP app does not take ACTION_DIAL: " + pkg, e)
            try {
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch == null) {
                    openPlayDetails(context, pkg)
                    return CallResult(false, provider.name + " could not be opened, Play Store opened")
                }
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                CallResult(
                    true,
                    provider.name + " opened, enter the number there and call"
                )
            } catch (e2: ActivityNotFoundException) {
                Log.w(TAG, "IP app launch failed: " + pkg, e2)
                CallResult(false, provider.name + " could not be opened")
            }
        }
    }

    private fun openPlayDetails(context: Context, packageName: String) {
        try {
            val market = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + packageName)
            )
            market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Play Store app missing, opening web Play page", e)
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
            )
            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(web)
            } catch (e2: ActivityNotFoundException) {
                Log.w(TAG, "No browser to open Play page", e2)
                Toast.makeText(context, "Play Store could not be opened", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPlaySearch(context: Context, query: String) {
        try {
            val market = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://search?q=" + Uri.encode(query))
            )
            market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Play Store app missing, opening web search", e)
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://play.google.com/store/search?q=" + Uri.encode(query)
                )
            )
            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(web)
            } catch (e2: ActivityNotFoundException) {
                Log.w(TAG, "No browser to open Play search", e2)
                Toast.makeText(context, "Play Store could not be opened", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun installedLabel(context: Context, provider: Provider): String {
        if (provider.isSim) {
            return "SIM"
        }
        val pkg = provider.packageName ?: return "Not verified"
        return if (isInstalled(context, pkg)) "Installed" else "Not installed"
    }
}
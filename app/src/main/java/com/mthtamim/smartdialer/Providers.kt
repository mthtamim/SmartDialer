package com.mthtamim.smartdialer

/**
 * One call route. Order here is the order shown on screen.
 *
 * packageName is null for Original SIM (system dialer) and for any
 * provider whose Play Store package name could not be verified.
 * playSearchQuery is the fallback used when the app is not installed:
 * a direct Play Store details page when the package is known, otherwise
 * a Play Store search for the app name.
 */
data class Provider(
    val id: String,
    val name: String,
    val subtitle: String,
    val badge: String,
    val packageName: String?,
    val playSearchQuery: String,
    val isSim: Boolean = false,
    val isIp: Boolean = false,
    val initials: String
)

object Providers {
    const val SIM_ID = "sim"
    const val BRILLIANT_ID = "brilliant"
    const val AMBER_ID = "amber"
    const val ORBI_ID = "orbiltalk"
    const val ALAAP_ID = "alaap"
    const val DIAL_ID = "dial"
    const val AGNI_ID = "agni"
    const val ALO_ID = "alo"

    val ALL: List<Provider> = listOf(
        Provider(
            id = SIM_ID,
            name = "Original SIM",
            subtitle = "Normal Dialer",
            badge = "SIM",
            packageName = null,
            playSearchQuery = "",
            isSim = true,
            initials = "SI"
        ),
        Provider(
            id = BRILLIANT_ID,
            name = "Brilliant",
            subtitle = "InterCloud",
            badge = "IP",
            packageName = "com.brilliant.connect.com.bd",
            playSearchQuery = "com.brilliant.connect.com.bd",
            isIp = true,
            initials = "BR"
        ),
        Provider(
            id = AMBER_ID,
            name = "Amber IT",
            subtitle = "Amber IT IP Phone",
            badge = "IP",
            packageName = "com.amberit.dialer",
            playSearchQuery = "com.amberit.dialer",
            isIp = true,
            initials = "AM"
        ),
        Provider(
            id = ORBI_ID,
            name = "OrbiTalk",
            subtitle = "Race Online",
            badge = "IP",
            packageName = "com.orbitalk.app",
            playSearchQuery = "com.orbitalk.app",
            isIp = true,
            initials = "OR"
        ),
        Provider(
            id = ALAAP_ID,
            name = "BTCL Alaap",
            subtitle = "BTCL",
            badge = "IP",
            packageName = "com.alaap.app",
            playSearchQuery = "com.alaap.app",
            isIp = true,
            initials = "AL"
        ),
        Provider(
            id = DIAL_ID,
            name = "Dial",
            subtitle = "Link3",
            badge = "IP",
            packageName = "com.beint.dial",
            playSearchQuery = "com.beint.dial",
            isIp = true,
            initials = "DI"
        ),
        Provider(
            id = AGNI_ID,
            name = "AgniTalk",
            subtitle = "Agni Systems",
            badge = "IP",
            packageName = "com.agni.app",
            playSearchQuery = "com.agni.app",
            isIp = true,
            initials = "AG"
        ),
        Provider(
            id = ALO_ID,
            name = "Alo Talk",
            subtitle = "Alo Communications",
            badge = "IP",
            packageName = null,
            playSearchQuery = "Alo Talk IP Phone",
            isIp = true,
            initials = "AT"
        )
    )

    fun byId(id: String): Provider {
        return ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}

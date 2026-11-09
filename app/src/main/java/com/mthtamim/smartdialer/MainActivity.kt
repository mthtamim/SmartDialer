package com.mthtamim.smartdialer

import android.Manifest
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val dialled = StringBuilder()
    private var selectedProviderId: String = Providers.SIM_ID
    private var contacts: List<PhoneContact> = emptyList()
    private var currentTab = 0

    private lateinit var autoCallPill: TextView
    private lateinit var defaultRouteLabel: TextView
    private lateinit var providerList: RecyclerView
    private lateinit var providerAdapter: ProviderAdapter
    private lateinit var simRow: LinearLayout
    private lateinit var sim1Button: TextView
    private lateinit var sim2Button: TextView
    private lateinit var numberDisplay: TextView
    private lateinit var suggestionScroll: View
    private lateinit var suggestionRow: LinearLayout
    private lateinit var dialGrid: LinearLayout
    private lateinit var dialPadSection: View
    private lateinit var backspaceButton: ImageButton
    private lateinit var callButton: TextView
    private lateinit var dialerView: View
    private lateinit var recentsView: View
    private lateinit var contactsView: View
    private lateinit var recentsList: RecyclerView
    private lateinit var recentsAdapter: RecentsAdapter
    private lateinit var recentsEmpty: TextView
    private lateinit var contactSearch: EditText
    private lateinit var favList: RecyclerView
    private lateinit var favAdapter: ContactsAdapter
    private lateinit var allList: RecyclerView
    private lateinit var allAdapter: ContactsAdapter
    private lateinit var favHeader: TextView
    private lateinit var allHeader: TextView
    private lateinit var contactsEmpty: TextView
    private lateinit var tabDialer: TextView
    private lateinit var tabRecents: TextView
    private lateinit var tabContacts: TextView

    private var telephonyManager: TelephonyManager? = null
    private var callListener: PhoneStateListener? = null
    private var wasOffhook = false
    private var callStartMillis = 0L

    private val requestCodePermissions = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectedProviderId = Store.defaultProviderId(this)
        bindViews()
        setupProviderList()
        setupDialPad()
        setupTabs()
        setupRecents()
        setupContacts()
        setupTopBar()

        showTab(0)
        refreshNumberDisplay()
        refreshSimRow()
        requestMissingPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshProviderList()
        refreshRecents()
        reloadContacts()
        startCallListener()
    }

    override fun onPause() {
        super.onPause()
        stopCallListener()
    }

    private fun bindViews() {
        autoCallPill = findViewById(R.id.autoCallPill)
        defaultRouteLabel = findViewById(R.id.defaultRouteLabel)
        providerList = findViewById(R.id.providerList)
        simRow = findViewById(R.id.simRow)
        sim1Button = findViewById(R.id.sim1Button)
        sim2Button = findViewById(R.id.sim2Button)
        numberDisplay = findViewById(R.id.numberDisplay)
        suggestionScroll = findViewById(R.id.suggestionScroll)
        suggestionRow = findViewById(R.id.suggestionRow)
        dialGrid = findViewById(R.id.dialGrid)
        dialPadSection = findViewById(R.id.dialPadSection)
        backspaceButton = findViewById(R.id.backspaceButton)
        callButton = findViewById(R.id.callButton)
        dialerView = findViewById(R.id.dialerView)
        recentsView = findViewById(R.id.recentsView)
        contactsView = findViewById(R.id.contactsView)
        recentsList = findViewById(R.id.recentsList)
        recentsEmpty = findViewById(R.id.recentsEmpty)
        contactSearch = findViewById(R.id.contactSearch)
        favList = findViewById(R.id.favList)
        favAdapter = ContactsAdapter(
            emptyList(),
            { number -> Store.isFavourite(this@MainActivity, number) },
            { contact -> fillNumber(contact.number) },
            { contact -> toggleFavourite(contact) }
        )
        allList = findViewById(R.id.allList)
        allAdapter = ContactsAdapter(
            emptyList(),
            { number -> Store.isFavourite(this@MainActivity, number) },
            { contact -> fillNumber(contact.number) },
            { contact -> toggleFavourite(contact) }
        )
        favHeader = findViewById(R.id.favHeader)
        allHeader = findViewById(R.id.allHeader)
        contactsEmpty = findViewById(R.id.contactsEmpty)
        tabDialer = findViewById(R.id.tabDialer)
        tabRecents = findViewById(R.id.tabRecents)
        tabContacts = findViewById(R.id.tabContacts)
    }

    // ---------- top bar ----------

    private fun setupTopBar() {
        refreshAutoCallPill()
        autoCallPill.setOnClickListener {
            val next = !Store.autoCallOn(this@MainActivity)
            Store.setAutoCallOn(this@MainActivity, next)
            refreshAutoCallPill()
            val message = if (next) {
                getString(R.string.auto_call_on)
            } else {
                getString(R.string.auto_call_off)
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener { openSettings() }
        sim1Button.setOnClickListener { setSimSlot(0) }
        sim2Button.setOnClickListener { setSimSlot(1) }
    }

    private fun refreshAutoCallPill() {
        val on = Store.autoCallOn(this)
        autoCallPill.text = if (on) {
            getString(R.string.auto_call_on_pill)
        } else {
            getString(R.string.auto_call_off_pill)
        }
        autoCallPill.setBackgroundResource(
            if (on) R.drawable.bg_pill else R.drawable.bg_pill_off
        )
        refreshDefaultRouteLabel()
    }

    private fun refreshDefaultRouteLabel() {
        val provider = Providers.byId(Store.defaultProviderId(this))
        val route = getString(R.string.default_route, provider.name)
        defaultRouteLabel.text = route
    }

    private fun setSimSlot(slot: Int) {
        Store.setSimSlot(this, slot)
        refreshSimRow()
    }

    private fun refreshSimRow() {
        val show = Providers.byId(selectedProviderId).isSim
        simRow.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) {
            return
        }
        val slot = Store.simSlot(this)
        sim1Button.setBackgroundResource(
            if (slot == 0) R.drawable.bg_card_selected else R.drawable.bg_card
        )
        sim2Button.setBackgroundResource(
            if (slot == 1) R.drawable.bg_card_selected else R.drawable.bg_card
        )
        // Selected SIM: green text + green-stroke card. Unselected: white
        // text on the plain dark card. (v1.0.0: both were solid green
        // because Button backgroundTint overrode both drawables.)
        sim1Button.setTextColor(
            getColor(if (slot == 0) R.color.sd_accent else R.color.sd_text)
        )
        sim2Button.setTextColor(
            getColor(if (slot == 1) R.color.sd_accent else R.color.sd_text)
        )
    }

    // ---------- provider list ----------

    private fun setupProviderList() {
        providerList.layoutManager = LinearLayoutManager(this)
        providerAdapter = ProviderAdapter(
            Providers.ALL,
            { provider -> CallHelper.isInstalled(this@MainActivity, provider.packageName ?: "") },
            Store.defaultProviderId(this),
            selectedProviderId,
            { provider -> onProviderTap(provider) },
            { provider -> onProviderLongPress(provider) }
        )
        providerList.adapter = providerAdapter
    }

    private fun refreshProviderList() {
        providerAdapter = ProviderAdapter(
            Providers.ALL,
            { provider -> CallHelper.isInstalled(this@MainActivity, provider.packageName ?: "") },
            Store.defaultProviderId(this),
            selectedProviderId,
            { provider -> onProviderTap(provider) },
            { provider -> onProviderLongPress(provider) }
        )
        providerList.adapter = providerAdapter
    }

    private fun onProviderTap(provider: Provider) {
        selectedProviderId = provider.id
        refreshSimRow()
        refreshProviderList()
        val number = dialled.toString()
        if (number.isEmpty()) {
            return
        }
        callVia(provider)
    }

    private fun onProviderLongPress(provider: Provider) {
        Store.setDefaultProviderId(this, provider.id)
        selectedProviderId = provider.id
        refreshProviderList()
        refreshDefaultRouteLabel()
        Toast.makeText(
            this,
            getString(R.string.default_set, provider.name),
            Toast.LENGTH_SHORT
        ).show()
    }

    // ---------- dial pad ----------

    // v1.0.1: dial pad is 4 fixed rows of fixed-size TextViews inside the
    // dialGrid LinearLayout. v1.0.0 built plain buttons with 0x0 size and
    // weight inside a wrap-content grid, so every button measured 0x0 and
    // the pad was invisible on the phone. A plain button under this
    // Material3 theme gets backgroundTint=colorPrimary (green) over its
    // background drawable, so TextView is used deliberately here.
    // Fixed size 64dp, 4 rows of 64dp + 8dp margins = 288dp total,
    // always visible.
    private fun setupDialPad() {
        val keys = listOf(
            Pair("1", "অ"),
            Pair("2", "ABC"),
            Pair("3", "DEF"),
            Pair("4", "GHI"),
            Pair("5", "JKL"),
            Pair("6", "MNO"),
            Pair("7", "PQRS"),
            Pair("8", "TUV"),
            Pair("9", "WXYZ"),
            Pair("*", "P"),
            Pair("0", "+"),
            Pair("#", "W")
        )
        val density = resources.displayMetrics.density
        val keySize = (64 * density).toInt()
        val marginH = (6 * density).toInt()
        val marginV = (4 * density).toInt()
        for (rowKeys in keys.chunked(3)) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER
            row.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            for (key in rowKeys) {
                val button = TextView(this)
                button.text = key.first + "\n" + key.second
                button.textSize = 18f
                button.setTextColor(getColor(R.color.sd_text))
                button.setBackgroundResource(R.drawable.bg_dial_button)
                button.gravity = Gravity.CENTER
                val params = LinearLayout.LayoutParams(keySize, keySize)
                params.setMargins(marginH, marginV, marginH, marginV)
                button.layoutParams = params
                button.setOnClickListener { appendDigit(key.first) }
                if (key.first == "0") {
                    button.setOnLongClickListener {
                        appendDigit("+")
                        true
                    }
                }
                row.addView(button)
            }
            dialGrid.addView(row)
        }
        backspaceButton.setOnClickListener {
            if (dialled.isNotEmpty()) {
                dialled.deleteCharAt(dialled.length - 1)
                refreshNumberDisplay()
            }
        }
        backspaceButton.setOnLongClickListener {
            dialled.clear()
            refreshNumberDisplay()
            true
        }
        callButton.setOnClickListener {
            callVia(Providers.byId(selectedProviderId))
        }
    }

    private fun appendDigit(digit: String) {
        if (dialled.length >= 15) {
            return
        }
        dialled.append(digit)
        refreshNumberDisplay()
    }

    private fun fillNumber(number: String) {
        dialled.clear()
        dialled.append(Store.normalize(number))
        refreshNumberDisplay()
        showTab(0)
    }

    private fun refreshNumberDisplay() {
        val number = dialled.toString()
        numberDisplay.text = number
        refreshSuggestions(number)
    }

    private fun refreshSuggestions(typed: String) {
        suggestionRow.removeAllViews()
        val matches = ContactsHelper.suggestions(contacts, typed)
        if (matches.isEmpty()) {
            suggestionScroll.visibility = View.GONE
            return
        }
        suggestionScroll.visibility = View.VISIBLE
        for (contact in matches) {
            val chip = TextView(this)
            chip.text = contact.name + " · " + contact.number
            chip.setTextColor(getColor(R.color.sd_text))
            chip.setBackgroundResource(R.drawable.bg_chip)
            val padding = (10 * resources.displayMetrics.density).toInt()
            chip.setPadding(padding, padding / 2, padding, padding / 2)
            chip.setOnClickListener { fillNumber(contact.number) }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, padding, 0)
            chip.layoutParams = params
            suggestionRow.addView(chip)
        }
    }

    // ---------- calling ----------

    private fun callVia(provider: Provider) {
        val number = dialled.toString()
        if (number.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_number_first), Toast.LENGTH_SHORT).show()
            return
        }
        val name = ContactsHelper.nameForNumber(contacts, number)
        val result = CallHelper.placeCall(
            this@MainActivity,
            number,
            provider,
            Store.autoCallOn(this@MainActivity),
            Store.simSlot(this@MainActivity),
            name
        )
        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
        refreshRecents()
    }

    // ---------- tabs ----------

    private fun setupTabs() {
        tabDialer.setOnClickListener { showTab(0) }
        tabRecents.setOnClickListener { showTab(1) }
        tabContacts.setOnClickListener { showTab(2) }
    }

    private fun showTab(tab: Int) {
        currentTab = tab
        dialerView.visibility = if (tab == 0) View.VISIBLE else View.GONE
        // Dial pad + call row live outside the weighted area and are only
        // shown on the Dialer tab, so they can never be pushed off-screen.
        dialPadSection.visibility = if (tab == 0) View.VISIBLE else View.GONE
        recentsView.visibility = if (tab == 1) View.VISIBLE else View.GONE
        contactsView.visibility = if (tab == 2) View.VISIBLE else View.GONE
        tabDialer.setBackgroundResource(
            if (tab == 0) R.drawable.bg_card_selected else R.drawable.bg_card
        )
        tabRecents.setBackgroundResource(
            if (tab == 1) R.drawable.bg_card_selected else R.drawable.bg_card
        )
        tabContacts.setBackgroundResource(
            if (tab == 2) R.drawable.bg_card_selected else R.drawable.bg_card
        )
        // v1.0.0: tab labels were invisible (green Button tint + clipped
        // drawableTop text). TextViews now, selected tab green text,
        // unselected tabs grey text on the dark card — like the preview.
        // Icons are tinted the same way (selected green, others grey).
        tabDialer.setTextColor(
            getColor(if (tab == 0) R.color.sd_accent else R.color.sd_text_secondary)
        )
        tabRecents.setTextColor(
            getColor(if (tab == 1) R.color.sd_accent else R.color.sd_text_secondary)
        )
        tabContacts.setTextColor(
            getColor(if (tab == 2) R.color.sd_accent else R.color.sd_text_secondary)
        )
        tabDialer.compoundDrawableTintList = ColorStateList.valueOf(
            getColor(if (tab == 0) R.color.sd_accent else R.color.sd_text_secondary)
        )
        tabRecents.compoundDrawableTintList = ColorStateList.valueOf(
            getColor(if (tab == 1) R.color.sd_accent else R.color.sd_text_secondary)
        )
        tabContacts.compoundDrawableTintList = ColorStateList.valueOf(
            getColor(if (tab == 2) R.color.sd_accent else R.color.sd_text_secondary)
        )
        if (tab == 1) {
            refreshRecents()
        }
        if (tab == 2) {
            reloadContacts()
        }
    }

    // ---------- recents ----------

    private fun setupRecents() {
        recentsList.layoutManager = LinearLayoutManager(this)
        recentsAdapter = RecentsAdapter(
            Store.recents(this),
            { entry -> fillNumber(entry.number) },
            { entry -> callVia(Providers.byId(entry.providerId)) }
        )
        recentsList.adapter = recentsAdapter
        val clearButton = findViewById<TextView>(R.id.clearRecentsButton)
        clearButton.setOnClickListener {
            Store.clearRecents(this@MainActivity)
            refreshRecents()
        }
    }

    private fun refreshRecents() {
        val entries = Store.recents(this)
        recentsAdapter.update(entries)
        recentsEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------- contacts ----------

    private fun setupContacts() {
        favList.layoutManager = LinearLayoutManager(this)
        favList.adapter = favAdapter
        allList.layoutManager = LinearLayoutManager(this)
        allList.adapter = allAdapter
        contactSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                applyContactFilter(s.toString())
            }
        })
    }

    private fun reloadContacts() {
        contacts = ContactsHelper.loadAll(this)
        applyContactFilter(contactSearch.text.toString())
    }

    private fun applyContactFilter(query: String) {
        val clean = query.trim()
        val filtered = if (clean.isEmpty()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(clean, ignoreCase = true) ||
                    Store.normalize(contact.number).contains(Store.normalize(clean))
            }
        }
        val favourites = filtered.filter { contact ->
            Store.isFavourite(this, contact.number)
        }
        val rest = filtered.filter { contact ->
            !Store.isFavourite(this, contact.number)
        }
        favAdapter.update(favourites)
        allAdapter.update(rest)
        favHeader.visibility = if (favourites.isEmpty()) View.GONE else View.VISIBLE
        favList.visibility = if (favourites.isEmpty()) View.GONE else View.VISIBLE
        allHeader.visibility = if (rest.isEmpty()) View.GONE else View.VISIBLE
        contactsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toggleFavourite(contact: PhoneContact) {
        val nowFav = !Store.isFavourite(this, contact.number)
        Store.setFavourite(this, contact.number, nowFav)
        applyContactFilter(contactSearch.text.toString())
        val message = if (nowFav) {
            getString(R.string.fav_added, contact.name)
        } else {
            getString(R.string.fav_removed, contact.name)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ---------- settings ----------

    private fun openSettings() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_settings, null)
        val defaultGroup = view.findViewById<RadioGroup>(R.id.settingsDefaultGroup)
        val radioIds = HashMap<Int, String>()
        val currentDefault = Store.defaultProviderId(this)
        for (provider in Providers.ALL) {
            val radio = RadioButton(this)
            radio.text = provider.name
            radio.setTextColor(getColor(R.color.sd_text))
            val id = View.generateViewId()
            radio.id = id
            radio.tag = provider.id
            radioIds[id] = provider.id
            if (provider.id == currentDefault) {
                radio.isChecked = true
            }
            defaultGroup.addView(radio)
        }
        val simGroup = view.findViewById<RadioGroup>(R.id.settingsSimGroup)
        if (Store.simSlot(this) == 0) {
            simGroup.check(R.id.settingsSim1)
        } else {
            simGroup.check(R.id.settingsSim2)
        }
        val autoCallSwitch = view.findViewById<Switch>(R.id.settingsAutoCall)
        autoCallSwitch.isChecked = Store.autoCallOn(this)
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.settings_title)
        builder.setView(view)
        builder.setPositiveButton(R.string.save) { _, _ ->
            val checkedId = defaultGroup.checkedRadioButtonId
            val chosen = radioIds[checkedId]
            if (chosen != null) {
                Store.setDefaultProviderId(this@MainActivity, chosen)
                selectedProviderId = chosen
            }
            val simChecked = simGroup.checkedRadioButtonId
            if (simChecked == R.id.settingsSim1) {
                Store.setSimSlot(this@MainActivity, 0)
            } else if (simChecked == R.id.settingsSim2) {
                Store.setSimSlot(this@MainActivity, 1)
            }
            Store.setAutoCallOn(this@MainActivity, autoCallSwitch.isChecked)
            refreshAutoCallPill()
            refreshSimRow()
            refreshProviderList()
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    // ---------- permissions ----------

    private fun requestMissingPermissions() {
        val needed = ArrayList<String>()
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            needed.add(Manifest.permission.CALL_PHONE)
        }
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            needed.add(Manifest.permission.READ_CONTACTS)
        }
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            needed.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                needed.toTypedArray(),
                requestCodePermissions
            )
        } else {
            reloadContacts()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            reloadContacts()
            refreshProviderList()
        }
    }

    // ---------- call duration ----------

    @Suppress("DEPRECATION")
    private fun startCallListener() {
        if (callListener != null) {
            return
        }
        try {
            val manager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager = manager
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_OFFHOOK -> {
                            wasOffhook = true
                            callStartMillis = System.currentTimeMillis()
                        }
                        TelephonyManager.CALL_STATE_IDLE -> {
                            if (wasOffhook && callStartMillis > 0) {
                                val seconds =
                                    (System.currentTimeMillis() - callStartMillis) / 1000
                                Store.updateLastRecentDuration(this@MainActivity, seconds)
                                refreshRecents()
                            }
                            wasOffhook = false
                            callStartMillis = 0L
                        }
                        else -> {
                        }
                    }
                }
            }
            callListener = listener
            manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            android.util.Log.w("SmartDialer", "Call listener needs READ_PHONE_STATE", e)
            callListener = null
        } catch (e: Exception) {
            android.util.Log.w("SmartDialer", "Call listener failed", e)
            callListener = null
        }
    }

    @Suppress("DEPRECATION")
    private fun stopCallListener() {
        val listener = callListener ?: return
        try {
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
        } catch (e: Exception) {
            android.util.Log.w("SmartDialer", "Call listener stop failed", e)
        }
        callListener = null
    }
}

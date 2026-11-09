package com.mthtamim.smartdialer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Provider card list on the Dialer tab. */
class ProviderAdapter(
    private val providers: List<Provider>,
    private val isInstalled: (Provider) -> Boolean,
    private val defaultId: String,
    private val selectedId: String,
    private val onTap: (Provider) -> Unit,
    private val onLongPress: (Provider) -> Unit
) : RecyclerView.Adapter<ProviderAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val initials: TextView = view.findViewById(R.id.providerInitials)
        val name: TextView = view.findViewById(R.id.providerName)
        val subtitle: TextView = view.findViewById(R.id.providerSubtitle)
        val badge: TextView = view.findViewById(R.id.providerBadge)
        val status: TextView = view.findViewById(R.id.providerStatus)
        val defaultMark: TextView = view.findViewById(R.id.providerDefault)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val provider = providers[position]
        holder.initials.text = provider.initials
        holder.name.text = provider.name
        holder.subtitle.text = provider.subtitle
        holder.badge.text = provider.badge
        holder.status.text = if (provider.isSim) {
            "SIM"
        } else if (isInstalled(provider)) {
            "Installed"
        } else {
            "Not installed"
        }
        holder.defaultMark.visibility = if (provider.id == defaultId) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val selected = provider.id == selectedId
        holder.itemView.setBackgroundResource(
            if (selected) R.drawable.bg_card_selected else R.drawable.bg_card
        )
        holder.itemView.setOnClickListener { onTap(provider) }
        holder.itemView.setOnLongClickListener {
            onLongPress(provider)
            true
        }
    }

    override fun getItemCount(): Int {
        return providers.size
    }
}

/** Recents tab. Newest first. Tap fills the number, tap the call
 * icon calls again through the same provider. */
class RecentsAdapter(
    private var entries: List<RecentEntry>,
    private val onTap: (RecentEntry) -> Unit,
    private val onCallAgain: (RecentEntry) -> Unit
) : RecyclerView.Adapter<RecentsAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.recentTitle)
        val detail: TextView = view.findViewById(R.id.recentDetail)
        val callAgain: ImageView = view.findViewById(R.id.recentCall)
    }

    fun update(newEntries: List<RecentEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = entries[position]
        holder.title.text = if (entry.name.isEmpty()) entry.number else entry.name
        val providerName = Providers.byId(entry.providerId).name
        val time = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
            .format(Date(entry.timeMillis))
        val duration = if (entry.durationSec > 0) {
            " · " + formatDuration(entry.durationSec)
        } else {
            ""
        }
        holder.detail.text = providerName + " · " + time + duration
        holder.itemView.setOnClickListener { onTap(entry) }
        holder.callAgain.setOnClickListener { onCallAgain(entry) }
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val rest = seconds % 60
        return if (minutes > 0) {
            minutes.toString() + ":" + String.format(Locale.US, "%02d", rest)
        } else {
            "0:" + String.format(Locale.US, "%02d", rest)
        }
    }
}

/** Contacts tab. Favourites are shown first by the activity, which
 * passes an already ordered list. */
class ContactsAdapter(
    private var contacts: List<PhoneContact>,
    private val isFavourite: (String) -> Boolean,
    private val onTap: (PhoneContact) -> Unit,
    private val onStar: (PhoneContact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.contactName)
        val number: TextView = view.findViewById(R.id.contactNumber)
        val star: ImageView = view.findViewById(R.id.contactStar)
    }

    fun update(newContacts: List<PhoneContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.number.text = contact.number
        holder.star.setImageResource(
            if (isFavourite(contact.number)) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star_big_off
            }
        )
        holder.itemView.setOnClickListener { onTap(contact) }
        holder.star.setOnClickListener { onStar(contact) }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }
}

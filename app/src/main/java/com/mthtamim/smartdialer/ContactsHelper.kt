package com.mthtamim.smartdialer

import android.content.Context
import android.provider.ContactsContract

data class PhoneContact(
    val name: String,
    val number: String
)

/**
 * Reads the phone's own contacts (READ_CONTACTS). Used for the Contacts
 * tab and for the suggestion chips shown while a number is typed.
 * Returns an empty list when the permission is missing, so the dialer
 * still works without contacts.
 */
object ContactsHelper {

    fun loadAll(context: Context): List<PhoneContact> {
        val out = ArrayList<PhoneContact>()
        val seen = HashSet<String>()
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            ) ?: return out
            cursor.use {
                val nameIndex = it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                val numberIndex = it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                while (it.moveToNext()) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                    val clean = Store.normalize(number)
                    if (clean.length < 3) {
                        continue
                    }
                    if (!seen.add(clean)) {
                        continue
                    }
                    out.add(PhoneContact(name.ifEmpty { number }, number))
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        return out
    }

    /** Up to 5 contacts whose number starts with the typed digits,
     * or whose name contains the typed text. */
    fun suggestions(contacts: List<PhoneContact>, typed: String): List<PhoneContact> {
        val clean = Store.normalize(typed)
        if (clean.length < 2) {
            return emptyList()
        }
        val out = ArrayList<PhoneContact>()
        for (contact in contacts) {
            if (out.size >= 5) {
                break
            }
            val contactNumber = Store.normalize(contact.number)
            if (contactNumber.startsWith(clean)) {
                out.add(contact)
            } else if (typed.length >= 2 && contact.name.contains(typed, ignoreCase = true)) {
                out.add(contact)
            }
        }
        return out
    }

    fun nameForNumber(contacts: List<PhoneContact>, number: String): String {
        val clean = Store.normalize(number)
        for (contact in contacts) {
            if (Store.normalize(contact.number) == clean) {
                return contact.name
            }
        }
        return ""
    }
}

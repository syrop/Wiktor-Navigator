/*
 * Copyright (C) 2017 Wiktor Nizio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.org.seva.navigator.view.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import javax.inject.Inject

import io.reactivex.subjects.PublishSubject
import pl.org.seva.navigator.R
import pl.org.seva.navigator.model.ContactsStore
import pl.org.seva.navigator.model.Contact

open class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    @Inject
    lateinit var contactsStore: ContactsStore

    private val clickSubject = PublishSubject.create<Contact>()
    private val longClickSubject = PublishSubject.create<Contact>()

    internal open fun getContact(position: Int): Contact {
        return contactsStore[position]
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int): ContactAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getContact(position)
        holder.name.text = contact.name!!
        holder.email.text = contact.email!!
        holder.view.setOnClickListener { onItemClick(position) }
        holder.view.setOnLongClickListener { onItemLongClick(position) }
    }

    private fun onItemClick(position: Int) {
        clickSubject.onNext(getContact(position))
    }

    private fun onItemLongClick(position: Int): Boolean {
        if (position == 0) {
            return false
        }
        longClickSubject.onNext(getContact(position))
        return true
    }

    fun addClickListener(contactClickListener: (contact : Contact) -> Unit) {
        clickSubject.subscribe { contactClickListener(it) }
    }

    fun addLongClickListener(contactLongClickListener: (contact : Contact) -> Unit) {
        longClickSubject.subscribe { contactLongClickListener.invoke(it) }
    }

    override fun getItemCount(): Int {
        return contactsStore.size()
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById<TextView>(R.id.name)
        val email: TextView = view.findViewById<TextView>(R.id.email)
        val view: View = view.findViewById(R.id.card_view)
    }
}

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

package pl.org.seva.navigator.contact

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import pl.org.seva.navigator.R
import pl.org.seva.navigator.main.instance

typealias ContactListener = (contact: Contact) -> Unit

open class ContactAdapter(private val listener: ContactListener? = null) :
        RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private val store: Contacts = instance()

    protected open fun getContact(position: Int) = store[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate())

    private fun ViewGroup.inflate() =
        LayoutInflater.from(context).inflate(LAYOUT, this, false)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getContact(position)
        holder.name.text = contact.name
        holder.email.text = contact.email
        holder.view.setOnClickListener { onItemClick(position) }
        holder.iconText.text = contact.name.substring(0, 1)
        holder.iconProfile.setImageResource(R.drawable.bg_circle)
        holder.iconProfile.setColorFilter(contact.color)
    }

    private fun onItemClick(position: Int) = listener?.invoke(getContact(position))

    override fun getItemCount() = store.size()

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val email: TextView = view.findViewById(R.id.email)
        val view: View = view.findViewById(R.id.view)
        val iconText: TextView = view.findViewById(R.id.icon_text)
        val iconProfile: ImageView = view.findViewById(R.id.icon_profile)
    }

    companion object {
        private val LAYOUT = R.layout.row_contact
    }
}

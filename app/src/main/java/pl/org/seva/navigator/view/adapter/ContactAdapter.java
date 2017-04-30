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

package pl.org.seva.navigator.view.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

import io.reactivex.subjects.PublishSubject;
import pl.org.seva.navigator.R;
import pl.org.seva.navigator.databinding.ContactBinding;
import pl.org.seva.navigator.model.ContactsCache;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.presenter.ContactClickListener;
import pl.org.seva.navigator.presenter.ContactLongClickListener;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    ContactsCache contactsCache;

    private final PublishSubject<Contact> clickSubject = PublishSubject.create();
    private final PublishSubject<Contact> longClickSubject = PublishSubject.create();

    Contact getContact(int position) {
        return contactsCache.get(position);
    }

    @Override
    public ContactAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ContactBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.contact,
                parent,
                false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact contact = getContact(position);
        holder.name.setText(contact.name());
        holder.email.setText(contact.email());
        holder.view.setOnClickListener(__ -> onItemClick(position));
        holder.view.setOnLongClickListener(__ -> onItemLongClick(position));
    }

    private void onItemClick(int position) {
        clickSubject.onNext(getContact(position));
    }

    private boolean onItemLongClick(int position) {
        if (position == 0) {
            return false;
        }
        longClickSubject.onNext(getContact(position));
        return true;
    }

    public void addClickListener(ContactClickListener contactClickListener) {
        clickSubject.subscribe(contactClickListener::onClick);
    }

    public void addLongClickListener(ContactLongClickListener contactLongClickListener) {
        longClickSubject.subscribe(contactLongClickListener::onLongClick);
    }

    @Override
    public int getItemCount() {
        return contactsCache.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView email;
        private final View view;

        private ViewHolder(ContactBinding binding) {
            super(binding.getRoot());
            name = binding.name;
            email = binding.email;
            view = binding.cardView;
        }
    }
}

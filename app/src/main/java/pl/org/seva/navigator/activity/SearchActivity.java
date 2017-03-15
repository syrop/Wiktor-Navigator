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

package pl.org.seva.navigator.activity;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.databinding.ActivitySearchBinding;
import pl.org.seva.navigator.manager.ContactManager;
import pl.org.seva.navigator.manager.FirebaseDatabaseManager;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.view.ContactAdapter;
import pl.org.seva.navigator.view.SingleContactAdapter;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;

    private ProgressDialog progress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_search);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            binding.notFoundLabel.setVisibility(View.GONE);
            binding.contacts.setVisibility(View.GONE);
            onSearchRequested();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void search(String query) {
        query = query.toLowerCase();
        progress = ProgressDialog.show(this, null, getString(R.string.search_searching));
        FirebaseDatabaseManager
                .getInstance()
                .readContactOnceForEmail(query)
                .subscribe(this::onContactReceived);
    }

    private void onContactReceived(Contact contact) {
        progress.cancel();
        if (contact.isEmpty()) {
            binding.notFoundLabel.setVisibility(View.VISIBLE);
            return;
        }
        binding.notFoundLabel.setVisibility(View.GONE);
        binding.contacts.setVisibility(View.VISIBLE);
        initRecyclerView(contact);
    }

    private void initRecyclerView(Contact contact) {
        RecyclerView rv = binding.contacts;
        rv.setHasFixedSize(true);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        ContactAdapter adapter = new SingleContactAdapter(contact);
        rv.setAdapter(adapter);
        adapter.clickListener().subscribe(this::onContactClick);
    }

    private void onContactClick(Contact contact) {
        ContactManager cm = ContactManager.getInstance();
        if (cm.contains(contact)) {
            finish();
            return;
        }
        new AlertDialog
                .Builder(this)
                .setCancelable(true)
                .setTitle(R.string.search_dialog_title)
                .setMessage(getString(R.string.search_dialog_question).replace("%s", contact.name()))
                .setPositiveButton(android.R.string.yes, ((dialog, which) -> contactApprovedAndFinish(contact)))
                .setNegativeButton(android.R.string.no, ((dialog, which) -> finish()))
                .create()
                .show();
    }

    private void contactApprovedAndFinish(Contact contact) {
        FirebaseDatabaseManager.getInstance().requestFriendship(contact);
        finish();
    }
}

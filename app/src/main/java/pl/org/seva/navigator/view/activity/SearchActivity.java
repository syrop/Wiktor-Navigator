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

package pl.org.seva.navigator.view.activity;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

import pl.org.seva.navigator.R;
import pl.org.seva.navigator.NavigatorApplication;
import pl.org.seva.navigator.model.ContactsCache;
import pl.org.seva.navigator.model.database.firebase.FirebaseReader;
import pl.org.seva.navigator.model.database.firebase.FirebaseWriter;
import pl.org.seva.navigator.databinding.ActivitySearchBinding;
import pl.org.seva.navigator.model.Contact;
import pl.org.seva.navigator.presenter.ContactClickListener;
import pl.org.seva.navigator.view.adapter.ContactAdapter;
import pl.org.seva.navigator.view.adapter.SingleContactAdapter;
import pl.org.seva.navigator.view.builder.dialog.FriendshipAddDialogBuilder;

public class SearchActivity extends AppCompatActivity implements ContactClickListener {

    private static final String IMAGE_PLACEHOLDER = "[image]";
    private static final int IMAGE_PLACEHOLDER_LENGTH = IMAGE_PLACEHOLDER.length();

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject FirebaseWriter firebaseWriter;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject FirebaseReader firebaseReader;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    ContactsCache contactsCache;

    private ActivitySearchBinding binding;
    private ProgressDialog progress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((NavigatorApplication) getApplication()).getGraph().inject(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_search);

        Intent intent = getIntent();
        //noinspection EqualsReplaceableByObjectsCall
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setLabelSpannableString(R.string.search_press_to_begin);
    }

    // http://stackoverflow.com/questions/3176033/spannablestring-with-image-example
    private void setLabelSpannableString(int id) {
        String str = getString(id);
        SpannableString ss = new SpannableString(str);
        Drawable d = getResources().getDrawable(R.drawable.ic_search_black_24dp);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
        int idPlaceholder = str.indexOf(IMAGE_PLACEHOLDER);
        if (idPlaceholder >= 0) {
            ss.setSpan(
                    span,
                    idPlaceholder,
                    idPlaceholder + IMAGE_PLACEHOLDER_LENGTH,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        binding.label.setText(ss);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        //noinspection EqualsReplaceableByObjectsCall
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY).trim();
            search(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_overflow_menu, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.collapseActionView();
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setOnSearchClickListener(__ -> onSearchClicked());

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnCloseListener(this::onSearchViewClosed);
        return true;
    }

    private boolean onSearchViewClosed() {
        if (binding.contacts.getVisibility() != View.VISIBLE) {
            binding.label.setVisibility(View.VISIBLE);
        }
        setLabelSpannableString(R.string.search_press_to_begin);
        return false;
    }

    private void onSearchClicked() {
        binding.label.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                binding.label.setVisibility(View.GONE);
                binding.contacts.setVisibility(View.GONE);
                onSearchRequested();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void search(String query) {
        query = query.toLowerCase();
        progress = ProgressDialog.show(this, null, getString(R.string.search_searching));
        firebaseReader
                .readContactOnceForEmail(query)
                .subscribe(this::onContactReceived);
    }

    private void onContactReceived(Contact contact) {
        progress.cancel();
        if (contact.isEmpty()) {
            binding.label.setVisibility(View.VISIBLE);
            binding.contacts.setVisibility(View.GONE);
            setLabelSpannableString(R.string.search_no_user_found);
            return;
        }
        binding.label.setVisibility(View.GONE);
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
        adapter.addClickListener(this);
    }

    @Override
    public void onClick(Contact contact) {
        if (contactsCache.contains(contact)) {
            finish();
            return;
        }
        new FriendshipAddDialogBuilder(this)
                .setContact(contact)
                .setYesAction(() -> contactApprovedAndFinish(contact))
                .setNoAction(this::finish)
                .build()
                .show();
    }

    private void contactApprovedAndFinish(Contact contact) {
        Toast.makeText(this, R.string.search_waiting_for_party, Toast.LENGTH_SHORT).show();
        firebaseWriter.requestFriendship(contact);
        finish();
    }
}

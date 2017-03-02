package pl.org.seva.navigator.view;

import pl.org.seva.navigator.model.Contact;

public class SingleContactAdapter extends ContactAdapter {

    private final Contact contact;

    public SingleContactAdapter(Contact contact) {
        super();
        this.contact = contact;
    }

    @Override
    Contact getContact(int position) {
        return contact;
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}

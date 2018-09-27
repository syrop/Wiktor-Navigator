package pl.org.seva.navigator.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pl.org.seva.navigator.contact.Contact

class NavigationViewModel : ViewModel() {

    val contact: MutableLiveData<Contact> = MutableLiveData()
}

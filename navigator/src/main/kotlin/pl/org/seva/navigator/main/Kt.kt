package pl.org.seva.navigator.main

import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentActivity

infix fun <T> Context.start(clazz: Class<T>): Boolean {
    startActivity(Intent(this, clazz))
    return true
}

fun <T> Context.start(clazz: Class<T>, f: Intent.() -> Intent): Boolean {
    startActivity(Intent(this, clazz).run(f))
    return true
}

fun <T> FragmentActivity.startForResult(clazz: Class<T>, requestCode: Int) {
    startActivityForResult(Intent(this, clazz), requestCode)
}

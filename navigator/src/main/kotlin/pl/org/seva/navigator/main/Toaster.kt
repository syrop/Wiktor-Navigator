package pl.org.seva.navigator.main

import android.content.Context
import android.widget.Toast

fun toaster() = instance<Toaster>()

class Toaster(private val ctx: Context) {

    fun toast(f: Context.() -> String) {
        val s = ctx.f()
        if (s.isNotBlank()) {
            Toast.makeText(ctx, ctx.f(), Toast.LENGTH_SHORT).show()
        }
    }
}

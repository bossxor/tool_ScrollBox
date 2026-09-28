package com.bossxor.scrollbox

import android.app.Application
import com.bossxor.scrollbox.data.AppDatabase
import com.bossxor.scrollbox.data.Prefs

class ScrollBoxApp : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = AppDatabase.get(this)
        prefs = Prefs(this)
    }

    companion object {
        lateinit var instance: ScrollBoxApp
            private set
    }
}

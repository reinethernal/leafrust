package com.leafrust

import android.app.Application
import com.leafrust.data.ai.LeafAnalyzer
import com.leafrust.data.db.InspectionRepository
import com.leafrust.util.AppLog

class LeafRustApp : Application() {
    lateinit var repository: InspectionRepository
        private set
    lateinit var analyzer: LeafAnalyzer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLog.init(this)
        AppLog.i("App", "LeafRust start · debug=${BuildConfig.DEBUG} · v=${BuildConfig.VERSION_NAME}")
        repository = InspectionRepository(this)
        analyzer = LeafAnalyzer(this)
    }

    companion object {
        lateinit var instance: LeafRustApp
            private set
    }
}

package com.nobodymusic.tyxypoor
import android.app.Application
import com.nobodymusic.tyxypoor.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
class App : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        scope.launch {
            val repo = ServiceLocator.repository
            val ready = runCatching {
                repo.sourcesOnce().any { it.enabled && it.script.isNotBlank() }
            }.getOrDefault(false)
            if (!ready) {
                runCatching { ServiceLocator.sourceManager.autoSelectBestSource() }
            }
        }
    }
}

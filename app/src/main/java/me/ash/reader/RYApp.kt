package me.ash.reader

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ash.reader.data.module.ApplicationScope
import me.ash.reader.data.module.IODispatcher
import me.ash.reader.data.repository.*
import me.ash.reader.data.source.OPMLDataSource
import me.ash.reader.data.source.RYDatabase
import me.ash.reader.data.source.RYNetworkDataSource
import me.ash.reader.ui.ext.del
import me.ash.reader.ui.ext.getLatestApk
import me.ash.reader.ui.ext.isFdroid
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * The Application class, where the Dagger components is generated.
 */
@HiltAndroidApp
class RYApp : Application(), Configuration.Provider {

    /**
     * From: [Feeder](https://gitlab.com/spacecowboy/Feeder).
     *
     * Install Conscrypt to handle TLSv1.3 pre Android10.
     */
    init {
        // Cancel TLSv1.3 support pre Android10
        // Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    @Inject
    lateinit var ryDatabase: RYDatabase

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var ryNetworkDataSource: RYNetworkDataSource

    @Inject
    lateinit var OPMLDataSource: OPMLDataSource

    @Inject
    lateinit var rssHelper: RssHelper

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var ryRepository: RYRepository

    @Inject
    lateinit var stringsRepository: StringsRepository

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var localRssRepository: LocalRssRepository

    @Inject
    lateinit var opmlRepository: OpmlRepository

    @Inject
    lateinit var rssRepository: RssRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    @IODispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var imageLoader: ImageLoader

    /**
     * When the application startup.
     *
     * 1. Set the uncaught exception handler
     * 2. Initialize the default account if there is none
     * 3. Synchronize once
     * 4. Check for new version
     */
    override fun onCreate() {
        super.onCreate()
        CrashHandler(this)
        applicationScope.launch {
            accountInit()
            workerInit()
            checkUpdate()
        }
    }

    /**
     * Override the [Configuration.Builder] to provide the [HiltWorkerFactory].
     */
    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    private suspend fun accountInit() {
        withContext(ioDispatcher) {
            if (accountRepository.isNoAccount()) {
                accountRepository.addDefaultAccount()
            }
        }
    }

    private suspend fun workerInit() {
        rssRepository.get().doSync(isOnStart = true)
    }

    private suspend fun checkUpdate() {
        if (isFdroid) return
        withContext(ioDispatcher) {
            applicationContext.getLatestApk().let {
                if (it.exists()) it.del()
            }
        }
        ryRepository.checkUpdate(showToast = false)
    }
}

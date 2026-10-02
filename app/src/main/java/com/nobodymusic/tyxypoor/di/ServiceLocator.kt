package com.nobodymusic.tyxypoor.di

import android.content.Context
import com.nobodymusic.tyxypoor.data.AppDatabase
import com.nobodymusic.tyxypoor.data.MusicRepository
import com.nobodymusic.tyxypoor.localmusic.LocalScanner
import com.nobodymusic.tyxypoor.source.SourceManager

object ServiceLocator {
    lateinit var appContext: Context
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var repository: MusicRepository
        private set
    lateinit var sourceManager: SourceManager
        private set
    lateinit var localScanner: LocalScanner
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        database = AppDatabase.get(appContext)
        repository = MusicRepository(database.songDao(), database.playlistDao(), database.sourceDao(), database.historyDao())
        sourceManager = SourceManager(repository)
        localScanner = LocalScanner(appContext)
    }
}
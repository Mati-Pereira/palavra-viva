package com.palavraviva.app

import android.app.Application
import com.palavraviva.app.data.bible.BibleImporter
import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.repositories.BibleRepository
import com.palavraviva.app.data.repositories.HighlightRepository
import com.palavraviva.app.data.repositories.PlanRepository
import com.palavraviva.app.data.repositories.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application do Palavra Viva: inicializa o banco e os repositórios em
 * [onCreate] (acesso global via companion object) e dispara a semente do
 * primeiro início em um escopo próprio com [SupervisorJob].
 */
class PalavraVivaApp : Application() {

    /** Escopo para trabalho em background que sobrevive às telas. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.build(this)
        bibleRepository = BibleRepository(database)
        planRepository = PlanRepository(database)
        progressRepository = ProgressRepository(database)
        highlightRepository = HighlightRepository(database)

        // Primeiro início: importa assets/bible/*.json e semeia planos.
        applicationScope.launch {
            BibleImporter.seed(this@PalavraVivaApp, database)
        }

        // Notificação diária (WorkManager) — classe do tópico de notificações.
        com.palavraviva.app.notification.NotificationScheduler.ensureScheduled(this)
    }

    companion object {
        lateinit var database: AppDatabase
            private set
        lateinit var bibleRepository: BibleRepository
            private set
        lateinit var planRepository: PlanRepository
            private set
        lateinit var progressRepository: ProgressRepository
            private set
        lateinit var highlightRepository: HighlightRepository
            private set
    }
}

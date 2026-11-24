package com.palavraviva.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.palavraviva.app.data.bible.BibleImporter
import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.prefs.ReminderPrefs
import com.palavraviva.app.data.repositories.BibleRepository
import com.palavraviva.app.data.repositories.HighlightRepository
import com.palavraviva.app.data.repositories.PlanRepository
import com.palavraviva.app.data.repositories.ProgressRepository
import com.palavraviva.app.data.repositories.StudyRepository
import com.palavraviva.app.data.study.GlossaryImporter
import com.palavraviva.app.notification.NotificationScheduler
import com.palavraviva.app.widget.VerseOfDayGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
        studyRepository = StudyRepository(this, database)

        // Primeiro início: importa assets/bible/*.json, semeia planos e glossário offline.
        // Ao final, atualiza o widget para trocar o fallback pelo versículo real.
        applicationScope.launch {
            BibleImporter.seed(this@PalavraVivaApp, database)
            GlossaryImporter.seed(this@PalavraVivaApp, database)
            runCatching { VerseOfDayGlanceWidget.updateAll(this@PalavraVivaApp) }
        }

        // Notificação diária (WorkManager), respeitando o toggle dos Ajustes.
        applicationScope.launch {
            if (ReminderPrefs.enabled(this@PalavraVivaApp).first()) {
                NotificationScheduler.ensureScheduled(this@PalavraVivaApp)
            }
        }
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
        lateinit var studyRepository: StudyRepository
            private set
    }
}

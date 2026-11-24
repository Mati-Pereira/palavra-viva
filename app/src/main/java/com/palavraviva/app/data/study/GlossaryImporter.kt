package com.palavraviva.app.data.study

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.GlossaryTerm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray

private val Context.studySeedStore by preferencesDataStore(name = "study_seed")

/**
 * Importador idempotente do glossário histórico-cultural offline em `assets/study/glossary.json`.
 * Usa versão de semente para permitir adicionar novos termos em futuras atualizações do app.
 */
object GlossaryImporter {

    private const val GLOSSARY_VERSION = 1
    private val KEY_GLOSSARY_VERSION = intPreferencesKey("glossary_version")

    suspend fun seed(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val dao = database.glossaryDao()
            val savedVersion = context.studySeedStore.data.first()[KEY_GLOSSARY_VERSION] ?: 0
            if (savedVersion >= GLOSSARY_VERSION && dao.count() > 0) return@withContext

            val jsonText = runCatching {
                context.assets.open("study/glossary.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrNull() ?: return@withContext

            val array = runCatching { JSONArray(jsonText) }.getOrNull() ?: return@withContext
            val terms = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val termKey = obj.optString("termKey", "").trim()
                    val term = obj.optString("term", "").trim()
                    val category = obj.optString("category", "").trim()
                    val summary = obj.optString("summary", "").trim()
                    val historicalContext = obj.optString("historicalContext", "").trim()
                    if (termKey.isEmpty() || term.isEmpty() || summary.isEmpty()) continue

                    val originalWord = if (!obj.isNull("originalWord")) {
                        obj.optString("originalWord", "").trim().takeIf { it.isNotEmpty() && it != "null" }
                    } else {
                        null
                    }
                    val aliasesArray = obj.optJSONArray("aliases")
                    val aliasesCsv = buildList {
                        if (aliasesArray != null) {
                            for (j in 0 until aliasesArray.length()) {
                                aliasesArray.optString(j, "").trim().takeIf { it.isNotEmpty() }?.let(::add)
                            }
                        }
                    }.joinToString(",")

                    add(
                        GlossaryTerm(
                            termKey = termKey,
                            term = term,
                            category = category.ifEmpty { "Cultura" },
                            originalWord = originalWord,
                            summary = summary,
                            historicalContext = historicalContext,
                            aliasesCsv = aliasesCsv,
                        ),
                    )
                }
            }

            if (terms.isNotEmpty()) {
                dao.insertAll(terms)
                context.studySeedStore.edit { it[KEY_GLOSSARY_VERSION] = GLOSSARY_VERSION }
            }
        }
    }
}

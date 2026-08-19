package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WordDefinition(
    val definition: String,
    val example: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList()
)

data class MeaningGroup(
    val partOfSpeech: String,
    val definitions: List<WordDefinition>,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList()
)

data class DictionaryEntry(
    val word: String,
    val phonetic: String? = null,
    val meanings: List<MeaningGroup> = emptyList(),
    val sourceUrl: String? = null
)

object DictionaryService {

    suspend fun lookupWord(word: String): Result<List<DictionaryEntry>> = withContext(Dispatchers.IO) {
        try {
            val cleanWord = word.trim().lowercase().filter { it.isLetter() || it == '-' }
            if (cleanWord.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid word"))
            }

            val encoded = URLEncoder.encode(cleanWord, "UTF-8")
            val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                return@withContext Result.failure(Exception("No definitions found for \"$word\""))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val jsonText = reader.use { it.readText() }
            val jsonArray = JSONArray(jsonText)

            val entries = mutableListOf<DictionaryEntry>()
            for (i in 0 until jsonArray.length()) {
                val entryObj = jsonArray.getJSONObject(i)
                val wordName = entryObj.optString("word", cleanWord)
                val phonetic = entryObj.optString("phonetic").ifEmpty {
                    val phoneticsArr = entryObj.optJSONArray("phonetics")
                    if (phoneticsArr != null && phoneticsArr.length() > 0) {
                        phoneticsArr.getJSONObject(0).optString("text").ifEmpty { null }
                    } else null
                }

                val meaningsList = mutableListOf<MeaningGroup>()
                val meaningsArr = entryObj.optJSONArray("meanings")
                if (meaningsArr != null) {
                    for (m in 0 until meaningsArr.length()) {
                        val mObj = meaningsArr.getJSONObject(m)
                        val partOfSpeech = mObj.optString("partOfSpeech", "general")
                        
                        val topSynonyms = mutableListOf<String>()
                        val synArr = mObj.optJSONArray("synonyms")
                        if (synArr != null) {
                            for (s in 0 until synArr.length()) {
                                val syn = synArr.optString(s)
                                if (syn.isNotBlank()) topSynonyms.add(syn)
                            }
                        }

                        val topAntonyms = mutableListOf<String>()
                        val antArr = mObj.optJSONArray("antonyms")
                        if (antArr != null) {
                            for (a in 0 until antArr.length()) {
                                val ant = antArr.optString(a)
                                if (ant.isNotBlank()) topAntonyms.add(ant)
                            }
                        }

                        val defsList = mutableListOf<WordDefinition>()
                        val defsArr = mObj.optJSONArray("definitions")
                        if (defsArr != null) {
                            for (d in 0 until defsArr.length()) {
                                val dObj = defsArr.getJSONObject(d)
                                val defText = dObj.optString("definition", "")
                                val example = dObj.optString("example").ifEmpty { null }
                                
                                val defSyns = mutableListOf<String>()
                                val dSynArr = dObj.optJSONArray("synonyms")
                                if (dSynArr != null) {
                                    for (ds in 0 until dSynArr.length()) {
                                        val syn = dSynArr.optString(ds)
                                        if (syn.isNotBlank()) defSyns.add(syn)
                                    }
                                }

                                val defAnts = mutableListOf<String>()
                                val dAntArr = dObj.optJSONArray("antonyms")
                                if (dAntArr != null) {
                                    for (da in 0 until dAntArr.length()) {
                                        val ant = dAntArr.optString(da)
                                        if (ant.isNotBlank()) defAnts.add(ant)
                                    }
                                }

                                if (defText.isNotBlank()) {
                                    defsList.add(
                                        WordDefinition(
                                            definition = defText,
                                            example = example,
                                            synonyms = defSyns,
                                            antonyms = defAnts
                                        )
                                    )
                                }
                            }
                        }

                        meaningsList.add(
                            MeaningGroup(
                                partOfSpeech = partOfSpeech,
                                definitions = defsList,
                                synonyms = topSynonyms,
                                antonyms = topAntonyms
                            )
                        )
                    }
                }

                entries.add(
                    DictionaryEntry(
                        word = wordName,
                        phonetic = phonetic,
                        meanings = meaningsList,
                        sourceUrl = "https://en.wiktionary.org/wiki/$encoded"
                    )
                )
            }

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

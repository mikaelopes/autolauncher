package com.mls.painel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Busca capa de álbum pelo nome da música quando a fonte (ex.: Bluetooth) não manda a imagem.
 * Usa a busca pública do iTunes (grátis, sem chave). Cada capa encontrada fica salva em disco,
 * então músicas repetidas mostram a capa mesmo sem internet. Roda sempre fora da thread principal.
 */
class ArtFetcher(ctx: Context) {
    private val dir = File(ctx.cacheDir, "capas").apply { mkdirs() }
    private val mem = LruCache<String, Bitmap>(40)
    private val misses = HashMap<String, Long>()

    fun key(title: String, artist: String): String =
        md5("${artist.trim().lowercase()}|${title.trim().lowercase()}")

    /** Só memória e disco, sem rede. Rápido. */
    fun cached(key: String): Bitmap? {
        mem.get(key)?.let { return it }
        val f = File(dir, "$key.jpg")
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.path)?.also { mem.put(key, it) }
        } catch (e: Exception) {
            null
        }
    }

    /** Pode acessar a rede. Chamar em background. */
    fun fetch(key: String, title: String, artist: String): Bitmap? {
        cached(key)?.let { return it }
        val last = synchronized(misses) { misses[key] }
        if (last != null && System.currentTimeMillis() - last < 10 * 60_000L) return null

        val queries = linkedSetOf(
            "$artist $title",
            "$artist ${clean(title)}",
            clean(title)
        ).map { it.trim() }.filter { it.length > 1 }

        for (q in queries) {
            val bmp = search(q) ?: continue
            mem.put(key, bmp)
            try {
                File(dir, "$key.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            } catch (e: Exception) {
            }
            return bmp
        }
        synchronized(misses) { misses[key] = System.currentTimeMillis() }
        return null
    }

    private fun search(term: String): Bitmap? {
        val q = URLEncoder.encode(term, "UTF-8")
        val json = get("https://itunes.apple.com/search?term=$q&entity=song&limit=1&country=BR") ?: return null
        return try {
            val results = JSONObject(String(json)).getJSONArray("results")
            if (results.length() == 0) return null
            val url = results.getJSONObject(0).optString("artworkUrl100").ifEmpty { return null }
                .replace("100x100bb", "400x400bb")
            val bytes = get(url) ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun get(url: String): ByteArray? {
        var c: HttpURLConnection? = null
        return try {
            c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 6000
            c.readTimeout = 8000
            if (c.responseCode != 200) null else c.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } finally {
            c?.disconnect()
        }
    }

    companion object {
        private val SUFFIX = Regex("\\s+-\\s+.*$")
        private val BRACKETS = Regex("\\s*[(\\[].*?[)\\]]")
        private val FEAT = Regex("\\s+(feat\\.?|ft\\.?|part\\.?)\\s+.*$", RegexOption.IGNORE_CASE)

        /** "Learning to Fly - Remix 2019" -> "Learning to Fly" */
        fun clean(t: String): String = t.replace(SUFFIX, "").replace(BRACKETS, "").replace(FEAT, "").trim()

        private fun md5(s: String): String =
            MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

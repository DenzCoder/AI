package id.asisten.app.domain.hanako

/**
 * The AI services Hanako can talk to. Each one has its own API key, its own model name and its own address.
 * Gemini speaks Google's `generateContent` protocol; Groq and DeepSeek speak the OpenAI-style chat completions one
 * ([gayaOpenAi]).
 *
 * The model names below are suggestions taken from each provider's documentation at the time of writing. They can
 * be retired, which is why the model name stays editable in the settings.
 */
enum class PenyediaAi(
    /** Stable id stored in DataStore and used in key names. Never change these values. */
    val kode: String,
    val nama: String,
    val modelBawaan: String,
    /** Model names offered as one-tap suggestions in the settings. The first is [modelBawaan]. */
    val saranModel: List<String>,
    /** Fixed base address. Requests only ever go here (HTTPS), so a key cannot be sent to another host. */
    val urlDasar: String,
    /** Where the user creates a key. Shown as plain text. */
    val halamanKunci: String,
    /** A reminder shown next to the consent switch when the provider handles data in a way worth knowing about. */
    val catatanData: String?,
) {
    GEMINI(
        kode = "gemini",
        nama = "Gemini",
        modelBawaan = NamaModel.BAWAAN,
        saranModel = listOf(NamaModel.BAWAAN),
        urlDasar = "https://generativelanguage.googleapis.com/v1beta",
        halamanKunci = "aistudio.google.com/apikey",
        catatanData = null,
    ),
    GROQ(
        kode = "groq",
        nama = "Groq",
        modelBawaan = "llama-3.3-70b-versatile",
        saranModel = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "openai/gpt-oss-20b", "openai/gpt-oss-120b"),
        urlDasar = "https://api.groq.com/openai/v1",
        halamanKunci = "console.groq.com/keys",
        catatanData = null,
    ),
    DEEPSEEK(
        kode = "deepseek",
        nama = "DeepSeek",
        modelBawaan = "deepseek-flash",
        saranModel = listOf("deepseek-flash", "deepseek-v4-pro"),
        urlDasar = "https://api.deepseek.com",
        halamanKunci = "platform.deepseek.com/api_keys",
        catatanData = "Menurut kebijakan privasinya, DeepSeek memproses dan menyimpan data di server di Tiongkok. " +
            "Pertimbangkan itu sebelum membagikan jadwal dan tugasmu.",
    ),
    ;

    val gayaOpenAi: Boolean get() = this != GEMINI

    companion object {
        /** Unknown or missing codes fall back to Gemini, the original choice. */
        fun dariKode(kode: String?): PenyediaAi = entries.firstOrNull { it.kode == kode } ?: GEMINI
    }
}

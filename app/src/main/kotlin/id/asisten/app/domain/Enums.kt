package id.asisten.app.domain

/**
 * Parses an enum name stored as text. Unknown or missing values fall back to [bawaan] instead of
 * crashing, so data written by a newer app version never breaks an older one.
 */
inline fun <reified E : Enum<E>> String?.keEnum(bawaan: E): E =
    if (this == null) bawaan else enumValues<E>().firstOrNull { it.name == this } ?: bawaan

package es.sebas1705.axiomnode.presentation.gameplay

private fun normalizeAccent(char: Char): Char = when (char) {
    'á', 'à', 'â', 'ä', 'ã', 'å', 'ā', 'ă', 'ą' -> 'a'
    'é', 'è', 'ê', 'ë', 'ē', 'ĕ', 'ė', 'ę', 'ě' -> 'e'
    'í', 'ì', 'î', 'ï', 'ĩ', 'ī', 'ĭ', 'į' -> 'i'
    'ó', 'ò', 'ô', 'ö', 'õ', 'ō', 'ŏ', 'ő' -> 'o'
    'ú', 'ù', 'û', 'ü', 'ũ', 'ū', 'ŭ', 'ů', 'ű', 'ų' -> 'u'
    'ý', 'ÿ' -> 'y'
    'ñ' -> 'n'
    'ç' -> 'c'
    else -> char
}

internal fun normalizeWordpassAnswer(raw: String): String {
    val lower = raw.trim().lowercase()
    val normalized = StringBuilder(lower.length)

    for (char in lower) {
        val mapped = normalizeAccent(char)
        when {
            mapped.isLetterOrDigit() -> normalized.append(mapped)
            mapped.isWhitespace() -> normalized.append(' ')
            else -> normalized.append(' ')
        }
    }

    return normalized
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun isWordpassAnswerMatch(userAnswer: String, correctAnswer: String): Boolean =
    normalizeWordpassAnswer(userAnswer) == normalizeWordpassAnswer(correctAnswer)

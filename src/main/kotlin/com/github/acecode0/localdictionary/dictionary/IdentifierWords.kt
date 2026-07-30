package com.github.acecode0.localdictionary.dictionary

object IdentifierWords {
    private val boundaries = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|_+")
    private val englishWord = Regex("[a-z]+")

    fun split(identifier: String): List<String> =
        identifier.split(boundaries)
            .map(String::lowercase)
            .filter(englishWord::matches)
}

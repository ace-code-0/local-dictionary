package com.github.acecode0.localdictionary.dictionary

import org.junit.Assert.assertEquals
import org.junit.Test

class IdentifierWordsTest {

    @Test
    fun splitsCamelPascalAndSnakeCase() {
        assertEquals(listOf("http", "cache", "key"), IdentifierWords.split("HTTP_CacheKey"))
    }

    @Test
    fun skipsEmptySegments() {
        assertEquals(listOf("local", "cache"), IdentifierWords.split("local__cache"))
    }
}

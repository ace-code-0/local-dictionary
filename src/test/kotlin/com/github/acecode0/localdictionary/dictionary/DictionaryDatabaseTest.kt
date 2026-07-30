package com.github.acecode0.localdictionary.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Path

class DictionaryDatabaseTest {

    @Test
    fun findsTranslationIgnoringCase() {
        DictionaryDatabase(testDatabasePath()).use { database ->
            assertEquals("n. 高速缓存", database.lookup("CACHE"))
        }
    }

    @Test
    fun returnsNullWhenWordIsMissing() {
        DictionaryDatabase(testDatabasePath()).use { database ->
            assertNull(database.lookup("missing"))
        }
    }

    private fun testDatabasePath(): Path =
        Path.of(requireNotNull(javaClass.getResource("/dictionary/test-ecdict.db")).toURI())
}

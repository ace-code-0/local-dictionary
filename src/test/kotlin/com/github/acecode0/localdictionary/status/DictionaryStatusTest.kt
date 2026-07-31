package com.github.acecode0.localdictionary.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryStatusTest {
    @Test
    fun reportsLatestDefinition() {
        val status = DictionaryStatus()

        status.reportDefinition("cache", "n. 高速缓存")

        assertEquals("cache: n. 高速缓存", status.current().text)
        assertFalse(status.current().hasError)
    }

    @Test
    fun reportsMissingDefinition() {
        val status = DictionaryStatus()

        status.reportMissing()

        assertEquals("未找到", status.current().text)
        assertFalse(status.current().hasError)
    }

    @Test
    fun reportsQueryError() {
        val status = DictionaryStatus()

        status.reportError(IllegalStateException("词典不可用"))

        assertEquals("错误", status.current().text)
        assertEquals("词典不可用", status.current().tooltip)
        assertTrue(status.current().hasError)
    }
}

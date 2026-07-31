package com.github.acecode0.localdictionary.status

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDictionaryStatusBarWidgetTest {
    @Test
    fun rendersCurrentStatus() {
        val status = DictionaryStatus().apply { reportDefinition("cache", "n. 高速缓存") }
        val widget = LocalDictionaryStatusBarWidget(status)

        assertEquals("cache: n. 高速缓存", widget.getText())
        assertEquals("cache: n. 高速缓存", widget.getTooltipText())
    }

    @Test
    fun exposesLogClickOnlyForErrorState() {
        val status = DictionaryStatus()
        val widget = LocalDictionaryStatusBarWidget(status)

        assertEquals(null, widget.getClickConsumer())

        status.reportError(IllegalStateException("词典不可用"))

        assertEquals(false, widget.getClickConsumer() == null)
    }
}

package com.github.acecode0.localdictionary.status

import com.intellij.openapi.components.Service
import java.util.concurrent.CopyOnWriteArraySet

data class DictionaryStatusSnapshot(
    val text: String,
    val tooltip: String,
    val hasError: Boolean,
)

@Service(Service.Level.APP)
class DictionaryStatus {
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    @Volatile
    private var snapshot = DictionaryStatusSnapshot("Local Dictionary", "Local Dictionary", false)

    fun reportDefinition(word: String, translation: String) {
        update(DictionaryStatusSnapshot("$word: $translation", "$word: $translation", false))
    }

    fun reportMissing() {
        update(DictionaryStatusSnapshot("未找到", "未找到词典释义", false))
    }

    fun reportError(error: Throwable) {
        update(DictionaryStatusSnapshot("错误", error.message ?: error.javaClass.simpleName, true))
    }

    fun current(): DictionaryStatusSnapshot = snapshot

    fun addListener(listener: () -> Unit): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }

    private fun update(next: DictionaryStatusSnapshot) {
        snapshot = next
        listeners.forEach { it() }
    }
}

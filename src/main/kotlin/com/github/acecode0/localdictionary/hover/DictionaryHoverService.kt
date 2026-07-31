package com.github.acecode0.localdictionary.hover

import com.github.acecode0.localdictionary.dictionary.DictionaryDatabase
import com.github.acecode0.localdictionary.dictionary.IdentifierWords
import com.github.acecode0.localdictionary.status.DictionaryStatus
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.util.Alarm

@Service(Service.Level.APP)
class DictionaryHoverService : Disposable, EditorMouseMotionListener {
    private val status = service<DictionaryStatus>()
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val delayController = HoverDelayController(object : HoverScheduler {
        override fun cancelAll() {
            alarm.cancelAllRequests()
        }

        override fun schedule(delayMillis: Int, action: () -> Unit) {
            alarm.addRequest(action, delayMillis)
        }
    })

    init {
        EditorFactory.getInstance().eventMulticaster.addEditorMouseMotionListener(this, this)
    }

    override fun mouseMoved(event: EditorMouseEvent) {
        val editor = event.editor
        val identifier = identifierAt(editor, event.mouseEvent.point.x, event.mouseEvent.point.y) ?: return
        delayController.schedule {
            ApplicationManager.getApplication().executeOnPooledThread {
                runCatching {
                    DictionaryDatabase.openBundled().use { database ->
                        IdentifierWords.split(identifier).mapNotNull { word ->
                            database.lookup(word)?.let { word to it }
                        }
                    }
                }.onSuccess { definitions ->
                    if (definitions.isEmpty()) {
                        status.reportMissing()
                    } else {
                        val (word, translation) = definitions.first()
                        status.reportDefinition(word, translation)
                    }
                    if (definitions.isNotEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            HintManager.getInstance().showInformationHint(
                                editor,
                                definitions.joinToString("\n") { (word, translation) -> "$word: $translation" },
                            )
                        }
                    }
                }.onFailure { error ->
                    status.reportError(error)
                }
            }
        }
    }

    override fun mouseDragged(event: EditorMouseEvent) = Unit

    override fun dispose() = Unit

    private fun identifierAt(editor: Editor, x: Int, y: Int): String? {
        val offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(java.awt.Point(x, y)))
        val text = editor.document.charsSequence
        if (offset !in text.indices || !isIdentifierCharacter(text[offset])) return null

        var start = offset
        var end = offset + 1
        while (start > 0 && isIdentifierCharacter(text[start - 1])) start--
        while (end < text.length && isIdentifierCharacter(text[end])) end++
        return text.subSequence(start, end).toString()
    }

    private fun isIdentifierCharacter(character: Char) = character == '_' || character.isLetterOrDigit()
}

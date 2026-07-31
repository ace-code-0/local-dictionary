package com.github.acecode0.localdictionary.status

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.application.PathManager
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

class LocalDictionaryStatusBarWidget(
    private val status: DictionaryStatus,
) : StatusBarWidget, StatusBarWidget.TextPresentation {
    private var statusBar: StatusBar? = null
    private val listener = status.addListener {
        val bar = statusBar ?: return@addListener
        runCatching { ApplicationManager.getApplication() }
            .getOrNull()
            ?.invokeLater { bar.updateWidget(ID()) }
            ?: bar.updateWidget(ID())
    }

    override fun ID() = ID

    override fun getPresentation() = this

    override fun getAlignment() = 0f

    override fun getText() = status.current().text

    override fun getTooltipText() = status.current().tooltip

    override fun getClickConsumer(): Consumer<MouseEvent>? =
        if (status.current().hasError) Consumer { BrowserUtil.browse(PathManager.getLogPath()) } else null

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        listener.close()
        statusBar = null
    }

    private companion object {
        const val ID = "LocalDictionaryStatusBarWidget"
    }
}

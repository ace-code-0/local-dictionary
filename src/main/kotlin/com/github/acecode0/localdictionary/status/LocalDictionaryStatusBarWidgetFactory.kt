package com.github.acecode0.localdictionary.status

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class LocalDictionaryStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId() = "LocalDictionaryStatusBarWidget"

    override fun getDisplayName() = "Local Dictionary"

    override fun isAvailable(project: Project) = true

    override fun createWidget(project: Project): StatusBarWidget =
        LocalDictionaryStatusBarWidget(service<DictionaryStatus>())

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar) = true
}

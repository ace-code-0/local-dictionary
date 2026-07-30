package com.github.acecode0.localdictionary.hover

interface HoverScheduler {
    fun cancelAll()

    fun schedule(delayMillis: Int, action: () -> Unit)
}

class HoverDelayController(private val scheduler: HoverScheduler) {
    fun schedule(action: () -> Unit) {
        scheduler.cancelAll()
        scheduler.schedule(HOVER_DELAY_MILLIS, action)
    }

    private companion object {
        const val HOVER_DELAY_MILLIS = 250
    }
}

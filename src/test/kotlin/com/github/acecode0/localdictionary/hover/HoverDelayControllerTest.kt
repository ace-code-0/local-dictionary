package com.github.acecode0.localdictionary.hover

import org.junit.Assert.assertEquals
import org.junit.Test

class HoverDelayControllerTest {
    @Test
    fun schedulesActionAfter250Milliseconds() {
        val scheduler = RecordingScheduler()
        HoverDelayController(scheduler).schedule { }

        assertEquals(250, scheduler.delayMillis)
    }

    @Test
    fun cancelsPreviousActionBeforeSchedulingNextOne() {
        val scheduler = RecordingScheduler()
        val controller = HoverDelayController(scheduler)

        controller.schedule { }
        controller.schedule { }

        assertEquals(2, scheduler.cancelCount)
    }

    private class RecordingScheduler : HoverScheduler {
        var cancelCount = 0
        var delayMillis = 0

        override fun cancelAll() {
            cancelCount++
        }

        override fun schedule(delayMillis: Int, action: () -> Unit) {
            this.delayMillis = delayMillis
        }
    }
}

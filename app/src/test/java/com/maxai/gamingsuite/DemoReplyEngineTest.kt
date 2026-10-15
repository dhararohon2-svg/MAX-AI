package com.maxai.gamingsuite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoReplyEngineTest {
    @Test
    fun emptyMessageGetsPrompt() {
        assertEquals("Please type a message first.", DemoReplyEngine.reply("   "))
    }

    @Test
    fun greetingGetsGreetingResponse() {
        assertTrue(DemoReplyEngine.reply("hello").contains("Hello"))
    }

    @Test
    fun gamingQuestionGetsSafeGuidance() {
        assertTrue(DemoReplyEngine.reply("How do I improve gaming?").contains("Game Mode"))
    }
}

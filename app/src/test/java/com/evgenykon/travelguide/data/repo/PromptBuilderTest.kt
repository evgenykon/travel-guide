package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.util.MapPoi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val template = "Точка: {name}\nКоординаты: {lat}, {lng}\nОбъект: {object}\nЗаметка: {hint}"

    @Test
    fun substitutesAllPlaceholders() {
        val prompt = PromptBuilder.build(
            template = template,
            name = "Кремль",
            lat = 55.75212,
            lng = 37.61734,
            hint = "старая крепость",
            poi = MapPoi("Московский Кремль", "крепость")
        )

        assertTrue(prompt.contains("Точка: Кремль"))
        assertTrue(prompt.contains("Координаты: 55.75212, 37.61734"))
        assertTrue(prompt.contains("Объект: Московский Кремль — крепость"))
        assertTrue(prompt.contains("Заметка: старая крепость"))
        assertFalse(prompt.contains("{"))
    }

    @Test
    fun usesDashForEmptyValues() {
        val prompt = PromptBuilder.build(
            template = template,
            name = "",
            lat = 1.0,
            lng = 2.0,
            hint = "  ",
            poi = null
        )

        assertTrue(prompt.contains("Точка: —"))
        assertTrue(prompt.contains("Объект: —"))
        assertTrue(prompt.contains("Заметка: —"))
    }

    @Test
    fun keepsCustomTextAndPlaceholders() {
        val prompt = PromptBuilder.build(
            template = "Мой промпт без подстановок",
            name = "X",
            lat = 0.0,
            lng = 0.0,
            hint = "",
            poi = null
        )
        assertEquals("Мой промпт без подстановок", prompt)
    }
}

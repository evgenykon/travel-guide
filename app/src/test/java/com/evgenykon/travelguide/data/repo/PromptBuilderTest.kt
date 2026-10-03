package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.data.prefs.AppSettings
import com.evgenykon.travelguide.util.MapPoi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val template =
        "Точка: {name}\nАдрес: {address}\nГород, страна: {location}\n" +
            "Координаты: {lat}, {lng}\nОбъект: {object}\nЗаметка: {hint}"

    private val place = PlaceInfo(
        address = "улица Абовяна, д. 1",
        city = "Ереван",
        country = "Армения"
    )

    @Test
    fun substitutesAllPlaceholders() {
        val prompt = PromptBuilder.build(
            template = template,
            name = "Мост Победы",
            place = place,
            lat = 40.18123,
            lng = 44.51123,
            hint = "старый мост",
            poi = MapPoi("Мост Победы", "мост")
        )

        assertTrue(prompt.contains("Точка: Мост Победы"))
        assertTrue(prompt.contains("Адрес: улица Абовяна, д. 1"))
        assertTrue(prompt.contains("Город, страна: Ереван, Армения"))
        assertTrue(prompt.contains("Координаты: 40.18123, 44.51123"))
        assertTrue(prompt.contains("Объект: Мост Победы — мост"))
        assertTrue(prompt.contains("Заметка: старый мост"))
        assertFalse(prompt.contains("{"))
    }

    @Test
    fun substitutesCityAndCountrySeparately() {
        val prompt = PromptBuilder.build(
            template = "{city}|{country}",
            name = "",
            place = place,
            lat = 0.0,
            lng = 0.0,
            hint = "",
            poi = null
        )
        assertEquals("Ереван|Армения", prompt)
    }

    @Test
    fun usesDashForEmptyValues() {
        val prompt = PromptBuilder.build(
            template = template,
            name = "",
            place = null,
            lat = 1.0,
            lng = 2.0,
            hint = "  ",
            poi = null
        )

        assertTrue(prompt.contains("Точка: —"))
        assertTrue(prompt.contains("Адрес: —"))
        assertTrue(prompt.contains("Город, страна: —"))
        assertTrue(prompt.contains("Объект: —"))
        assertTrue(prompt.contains("Заметка: —"))
    }

    @Test
    fun defaultTemplateForbidsSpeculationAndConfusesCities() {
        val template = AppSettings.DEFAULT_PROMPT_TEMPLATE
        assertTrue(template.contains("могли бы"))
        assertTrue(template.contains("не выдумывай"))
        assertTrue(template.contains("достоверные"))
        assertTrue(template.contains("озвучивается"))
        assertTrue(template.contains("не путай"))
        assertTrue(template.contains("{location}"))
        assertTrue(template.contains("{city}"))
        assertTrue(template.contains("{country}"))
        assertTrue(template.contains("{address}"))
        assertTrue(template.contains("{object}"))
        assertTrue(template.contains("{name}"))
        assertFalse(template.contains("Координаты: {lat}"))
    }
}

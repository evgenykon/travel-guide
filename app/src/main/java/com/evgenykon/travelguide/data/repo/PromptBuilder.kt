package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.util.MapPoi
import java.util.Locale

object PromptBuilder {

    fun build(
        template: String,
        name: String,
        lat: Double,
        lng: Double,
        hint: String,
        poi: MapPoi?
    ): String {
        val objectText = poi
            ?.let { listOfNotNull(it.name, it.category).joinToString(" — ") }
            .orEmpty()
        return template
            .replace("{name}", name.ifBlank { "—" })
            .replace("{lat}", String.format(Locale.US, "%.5f", lat))
            .replace("{lng}", String.format(Locale.US, "%.5f", lng))
            .replace("{object}", objectText.ifBlank { "—" })
            .replace("{hint}", hint.ifBlank { "—" })
    }
}

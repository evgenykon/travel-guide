package com.evgenykon.travelguide.util

import org.maplibre.geojson.Feature

data class MapPoi(
    val name: String,
    val category: String?
)

object MapPoiExtractor {

    private val placeClasses = setOf(
        "country", "continent", "state", "province", "region", "county",
        "municipality", "city", "town", "village", "suburb", "quarter",
        "neighbourhood", "hamlet", "island", "archipelago"
    )

    private val specificClasses = setOf(
        "cafe", "restaurant", "bar", "pub", "fast_food", "ice_cream", "museum",
        "monument", "memorial", "attraction", "artwork", "viewpoint", "castle",
        "fort", "church", "cathedral", "chapel", "monastery", "mosque",
        "synagogue", "temple", "place_of_worship", "hotel", "hostel",
        "guest_house", "park", "garden", "theatre", "cinema", "library",
        "school", "university", "college", "hospital", "pharmacy", "bank",
        "atm", "marketplace", "supermarket", "mall", "station", "railway",
        "subway", "aerodrome", "airport", "harbor", "beach", "peak", "volcano",
        "bridge", "tower", "ruins", "archaeological_site", "townhall",
        "courthouse", "embassy", "police", "fire_station", "post", "water",
        "river", "lake", "pond", "sea", "forest", "wood", "scrub", "grassland",
        "residential", "commercial", "industrial", "building"
    )

    private val categoryRu = mapOf(
        "cafe" to "кафе",
        "restaurant" to "ресторан",
        "bar" to "бар",
        "pub" to "паб",
        "fast_food" to "фастфуд",
        "ice_cream" to "кафе-мороженое",
        "museum" to "музей",
        "monument" to "памятник",
        "memorial" to "мемориал",
        "attraction" to "достопримечательность",
        "artwork" to "арт-объект",
        "viewpoint" to "смотровая площадка",
        "castle" to "замок",
        "fort" to "крепость",
        "church" to "церковь",
        "cathedral" to "собор",
        "chapel" to "часовня",
        "monastery" to "монастырь",
        "mosque" to "мечеть",
        "synagogue" to "синагога",
        "temple" to "храм",
        "place_of_worship" to "культовое сооружение",
        "hotel" to "отель",
        "hostel" to "хостел",
        "guest_house" to "гостевой дом",
        "park" to "парк",
        "garden" to "сад",
        "theatre" to "театр",
        "cinema" to "кинотеатр",
        "library" to "библиотека",
        "school" to "школа",
        "university" to "университет",
        "college" to "колледж",
        "hospital" to "больница",
        "pharmacy" to "аптека",
        "bank" to "банк",
        "atm" to "банкомат",
        "marketplace" to "рынок",
        "supermarket" to "супермаркет",
        "mall" to "торговый центр",
        "station" to "станция",
        "railway" to "железная дорога",
        "subway" to "метро",
        "aerodrome" to "аэродром",
        "airport" to "аэропорт",
        "harbor" to "порт",
        "beach" to "пляж",
        "peak" to "вершина",
        "volcano" to "вулкан",
        "bridge" to "мост",
        "tower" to "башня",
        "ruins" to "руины",
        "archaeological_site" to "археологический памятник",
        "townhall" to "мэрия",
        "courthouse" to "суд",
        "embassy" to "посольство",
        "police" to "полиция",
        "fire_station" to "пожарная станция",
        "post" to "почта",
        "water" to "водоём",
        "river" to "река",
        "lake" to "озеро",
        "pond" to "пруд",
        "sea" to "море",
        "forest" to "лес",
        "wood" to "лес",
        "scrub" to "кустарник",
        "grassland" to "луг",
        "residential" to "жилой район",
        "commercial" to "торговый район",
        "industrial" to "промышленный район",
        "building" to "здание"
    )

    private data class Candidate(val name: String, val category: String?, val priority: Int)

    fun extract(features: List<Feature>): MapPoi? {
        val candidates = features.mapNotNull { feature -> candidate(feature) }
        return candidates.minByOrNull { it.priority }?.let { MapPoi(it.name, it.category) }
    }

    private fun candidate(feature: Feature): Candidate? {
        if (feature.getStringProperty("id") != null) return null

        val name = feature.getStringProperty("name:ru")?.takeIf { it.isNotBlank() }
            ?: feature.getStringProperty("name")?.takeIf { it.isNotBlank() }
            ?: feature.getStringProperty("name:latin")?.takeIf { it.isNotBlank() }
            ?: return null

        val cls = feature.getStringProperty("class")?.lowercase()
        if (cls != null && cls in placeClasses) return null

        return Candidate(name, categoryOf(feature), priority(cls))
    }

    private fun priority(cls: String?): Int = when {
        cls == null -> 4
        cls in specificClasses -> 0
        cls in setOf("building", "transportation", "highway", "railway", "aeroway") -> 1
        cls in setOf("water", "waterway", "natural", "landuse") -> 2
        else -> 3
    }

    private fun categoryOf(feature: Feature): String? {
        val raw = listOf(
            "class", "subclass", "type", "amenity", "tourism", "shop",
            "historic", "leisure", "natural", "waterway", "railway", "highway"
        ).firstNotNullOfOrNull { key ->
            feature.getStringProperty(key)?.takeIf { it.isNotBlank() }
        } ?: return null
        val normalized = raw.lowercase()
        return categoryRu[normalized] ?: normalized.replace('_', ' ')
    }
}

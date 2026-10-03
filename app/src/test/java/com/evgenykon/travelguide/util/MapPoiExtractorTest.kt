package com.evgenykon.travelguide.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

class MapPoiExtractorTest {

    private fun feature(vararg props: Pair<String, String>): Feature =
        Feature.fromGeometry(Point.fromLngLat(37.0, 55.0)).apply {
            props.forEach { (key, value) -> addStringProperty(key, value) }
        }

    @Test
    fun prefersRussianName() {
        val poi = MapPoiExtractor.extract(
            listOf(feature("name" to "Museum", "name:ru" to "Музей", "class" to "museum"))
        )
        assertEquals("Музей", poi?.name)
        assertEquals("музей", poi?.category)
    }

    @Test
    fun skipsPlacesLikeCities() {
        assertNull(
            MapPoiExtractor.extract(listOf(feature("name" to "Москва", "class" to "city")))
        )
    }

    @Test
    fun skipsOwnMarkers() {
        assertNull(
            MapPoiExtractor.extract(
                listOf(feature("id" to "12", "name" to "Моя точка", "class" to "cafe"))
            )
        )
    }

    @Test
    fun prefersSpecificObjectOverBuilding() {
        val poi = MapPoiExtractor.extract(
            listOf(
                feature("name" to "Дом быта", "class" to "building"),
                feature("name" to "Кофейня", "class" to "cafe")
            )
        )
        assertEquals("Кофейня", poi?.name)
        assertEquals("кафе", poi?.category)
    }

    @Test
    fun returnsNullWithoutNames() {
        assertNull(MapPoiExtractor.extract(listOf(feature("class" to "cafe"))))
        assertNull(MapPoiExtractor.extract(emptyList()))
    }

    @Test
    fun keepsUnknownCategoryAsReadableText() {
        val poi = MapPoiExtractor.extract(
            listOf(feature("name" to "Что-то", "class" to "something_special"))
        )
        assertEquals("something special", poi?.category)
    }
}

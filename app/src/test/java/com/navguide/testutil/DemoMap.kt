package com.navguide.testutil

import com.navguide.core.MapDoc
import java.io.File

/**
 * Loads the bundled demo map from the app assets folder when running JVM unit tests.
 * Works from the module working directory (app/) used by Gradle unit tests.
 */
object DemoMap {
    fun load(): MapDoc = MapDoc.parse(readDemoMapText())

    fun readDemoMapText(): String {
        val candidates = listOf(
            File("src/main/assets/demo_map.json"),
            File("app/src/main/assets/demo_map.json"),
            File("../app/src/main/assets/demo_map.json")
        )
        val f = candidates.firstOrNull { it.exists() }
            ?: error("demo_map.json not found; looked in: ${candidates.joinToString { it.absolutePath }}")
        return f.readText()
    }
}

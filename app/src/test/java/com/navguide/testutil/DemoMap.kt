package com.navguide.testutil

import com.navguide.core.MapDoc
import java.io.File

/**
 * Loads bundled map assets from the app assets folder when running JVM unit tests.
 * Works from the module working directory (app/) used by Gradle unit tests.
 */
object DemoMap {

    fun readAsset(name: String): String {
        val candidates = listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name"),
            File("../app/src/main/assets/$name")
        )
        val f = candidates.firstOrNull { it.exists() }
            ?: error("$name not found; looked in: ${candidates.joinToString { it.absolutePath }}")
        return f.readText()
    }

    fun load(): MapDoc = MapDoc.parse(readAsset("demo_map.json"))
}

package com.navguide.data

import android.content.Context
import com.navguide.core.MapDoc

/** Loads the bundled demo map (offline-first). */
object MapRepository {
    fun load(context: Context): MapDoc {
        val text = context.assets.open("demo_map.json").bufferedReader().use { it.readText() }
        return MapDoc.parse(text)
    }
}

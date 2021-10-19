package com.varabyte.kobweb.api

import java.nio.file.Path

/**
 * A interface for creating an [Apis] instance.
 *
 * This class is provided to help with reflective access by a Kobweb server. Users should never have to interact with it
 * directly. It is expected that a Kobweb project will generate an implementation for this at compile-time.
 */
@Suppress("unused") // Called by reflection
interface ApisFactory {
    fun create(dataRoot: Path): Apis
}
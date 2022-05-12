plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

dependencies {
    implementation("com.phodal.chapi:chapi-domain:1.5.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

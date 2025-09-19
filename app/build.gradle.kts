import java.util.Properties
import java.net.URL
import java.time.LocalDate

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.10"
}

// Load API keys
val apiKeys = Properties().apply {
    val apiKeysFile = rootProject.file("apikeys.properties")
    if (apiKeysFile.exists()) {
        load(apiKeysFile.inputStream())
    }
}

android {
    namespace = "com.lukesleeman.currencyconverter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lukesleeman.currencyconverter"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_KEY", "\"${apiKeys["EXCHANGE_RATE_API_KEY"]}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_KEY", "\"${apiKeys["EXCHANGE_RATE_API_KEY"]}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.10")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Custom task to generate DefaultRates.kt with fresh API data
tasks.register("generateDefaultRates") {
    group = "currency"
    description = "Fetches fresh exchange rates from API and generates DefaultRates.kt"

    doLast {
        val apiKey = apiKeys["EXCHANGE_RATE_API_KEY"] ?: error("EXCHANGE_RATE_API_KEY not found in apikeys.properties")
        val apiUrl = "https://v6.exchangerate-api.com/v6/$apiKey/latest/EUR"

        println("Fetching exchange rates from: $apiUrl")

        // Fetch data from API
        val connection = URL(apiUrl).openConnection()
        val response = connection.getInputStream().bufferedReader().use { reader -> reader.readText() }

        // Parse JSON response (simple string parsing since we don't have Gson in build script)
        val ratesStart = response.indexOf("\"conversion_rates\":{") + "\"conversion_rates\":{".length
        val ratesEnd = response.indexOf("}", ratesStart)
        val ratesJson = response.substring(ratesStart, ratesEnd)

        // Extract rates
        val rates = mutableMapOf<String, Double>()
        val ratePattern = Regex("\"([A-Z]{3})\":([0-9.]+)")
        ratePattern.findAll(ratesJson).forEach { match ->
            val currency = match.groupValues[1]
            val rate = match.groupValues[2].toDouble()
            rates[currency] = rate
        }

        // Ensure EUR is included
        rates["EUR"] = 1.0

        // Generate Kotlin code
        val currentDate = LocalDate.now()
        val kotlinCode = buildString {
            appendLine("package com.lukesleeman.currencyconverter.data")
            appendLine()
            appendLine("/**")
            appendLine(" * Default exchange rates for offline functionality.")
            appendLine(" * These rates are EUR-based (EUR = 1.0) and fetched from exchangerate-api.com on $currentDate.")
            appendLine(" */")
            appendLine("object DefaultRates {")
            appendLine()
            appendLine("    /**")
            appendLine("     * Provides fallback exchange rates when API is unavailable.")
            appendLine("     * All rates are relative to EUR (1 EUR equals X units of other currency).")
            appendLine("     */")
            appendLine("    fun getDefaultRates(): Map<String, Double> {")
            appendLine("        return mapOf(")

            // Sort rates for consistent output, with EUR first
            val sortedRates = rates.toList().sortedWith(compareBy<Pair<String, Double>> { it.first != "EUR" }.thenBy { it.first })

            sortedRates.forEachIndexed { index, (currency, rate) ->
                val comma = if (index < sortedRates.size - 1) "," else ""
                appendLine("            \"$currency\" to $rate$comma")
            }

            appendLine("        )")
            appendLine("    }")
            appendLine("}")
        }

        // Write to file
        val outputFile = file("src/main/java/com/lukesleeman/currencyconverter/data/DefaultRates.kt")
        outputFile.writeText(kotlinCode)

        println("Generated DefaultRates.kt with ${rates.size} currencies")
        println("File: ${outputFile.absolutePath}")
    }
}
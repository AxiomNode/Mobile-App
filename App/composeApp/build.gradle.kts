import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// ---------------------------------------------------------------------------
// Environment configuration helpers
// ---------------------------------------------------------------------------

/** Load properties for a given environment name. */
fun loadEnvProps(env: String): Properties = Properties().apply {
    val f = file("env/${env}.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    } else {
        logger.warn("⚠️  env/${env}.properties not found – using defaults from example")
        file("env/env.properties.example").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
}

/** Resolve environment from Gradle task graph (exec-time) or start params. */
fun resolveEnv(): String {
    // 1) Explicit property: -Paxiomnode.env=stg
    findProperty("axiomnode.env")?.toString()?.let { return it }

    // 2) Detect from requested task names (available at config time too)
    val taskNames = gradle.startParameter.taskNames
    for (task in taskNames) {
        val lower = task.lowercase()
        when {
            lower.contains("prod") -> return "prod"
            lower.contains("stg")  -> return "stg"
            lower.contains("dev")  -> return "dev"
        }
    }

    // 3) Fallback
    return "dev"
}

// Config-time env (for signing configs, etc.)
val activeEnv: String = resolveEnv()
val envProps = loadEnvProps(activeEnv)
val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

fun envVal(key: String, default: String = ""): String =
    envProps.getProperty(key, default).trim()

logger.lifecycle("🔧 AxiomNode config-time environment: $activeEnv")

// ---------------------------------------------------------------------------
// Generated AppConfig source file
// ---------------------------------------------------------------------------
val generatedDir = layout.buildDirectory.dir("generated/config/kotlin")
val generateConfigTask = tasks.register("generateAppConfig") {
    val outputDir = generatedDir.get().asFile
    outputs.dir(outputDir)
    // Always re-run: the right env depends on which variant is being built
    outputs.upToDateWhen { false }
    // Prevent Gradle build cache from reusing a stale config from a different env
    outputs.cacheIf { false }

    doLast {
        // Resolve env at EXECUTION time – prefer explicit property, then task names
        val execEnv = findProperty("axiomnode.env")?.toString()
            ?: run {
                // Use start parameter task names (reliable) instead of full task graph
                val taskNames = project.gradle.startParameter.taskNames
                taskNames.asSequence()
                    .map { it.lowercase() }
                    .firstNotNullOfOrNull { name ->
                        when {
                            name.contains("prod") -> "prod"
                            name.contains("stg")  -> "stg"
                            name.contains("dev")  -> "dev"
                            else -> null
                        }
                    }
            }
            ?: "dev"

        val props = loadEnvProps(execEnv)
        fun prop(key: String, default: String = ""): String =
            props.getProperty(key, default).trim()

        val pkg = "es.sebas1705.axiomnode.config"
        val dir = File(outputDir, pkg.replace('.', '/'))
        dir.mkdirs()
        File(dir, "GeneratedConfig.kt").writeText(
            """
            |package $pkg
            |
            |/**
            | * Auto-generated from env/${execEnv}.properties – DO NOT EDIT.
            | */
            |object GeneratedConfig {
            |    const val ENVIRONMENT = "${execEnv.uppercase()}"
            |    const val API_BASE_URL = "${prop("API_BASE_URL", "http://10.0.2.2:7005")}"
            |    const val AUTH_MODE = "${prop("AUTH_MODE", "dev")}"
            |    const val FIREBASE_API_KEY = "${prop("FIREBASE_API_KEY")}"
            |    const val FIREBASE_AUTH_DOMAIN = "${prop("FIREBASE_AUTH_DOMAIN")}"
            |    const val FIREBASE_PROJECT_ID = "${prop("FIREBASE_PROJECT_ID")}"
            |    const val FIREBASE_STORAGE_BUCKET = "${prop("FIREBASE_STORAGE_BUCKET")}"
            |    const val FIREBASE_MESSAGING_SENDER_ID = "${prop("FIREBASE_MESSAGING_SENDER_ID")}"
            |    const val FIREBASE_APP_ID = "${prop("FIREBASE_APP_ID")}"
            |    const val FIREBASE_MEASUREMENT_ID = "${prop("FIREBASE_MEASUREMENT_ID")}"
            |    const val GOOGLE_WEB_CLIENT_ID = "${prop("GOOGLE_WEB_CLIENT_ID")}"
            |}
            """.trimMargin()
        )
        logger.lifecycle("✅ GeneratedConfig.kt → env=$execEnv, API_BASE_URL=${prop("API_BASE_URL", "http://10.0.2.2:7005")}")
    }
}

// ---------------------------------------------------------------------------
// Plugins
// ---------------------------------------------------------------------------
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

// ---------------------------------------------------------------------------
// Kotlin Multiplatform
// ---------------------------------------------------------------------------
kotlin {
    // Suppress beta warning for expect/actual classes
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "es.sebas1705.axiomnode.composeapp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    jvm()

    sourceSets {
        // Register generated config source dir for all platforms
        commonMain {
            kotlin.srcDir(generatedDir)
        }

        commonMain.dependencies {
            // Lifecycle / ViewModel
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)

            // Compose
            implementation(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.animation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Image loading
            implementation(libs.kamel.image)
            implementation(libs.kamel.default)

            implementation(libs.kotlinx.datetime)

            // Room & SQLite (KMP)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)

            // Ktor engine
            implementation(libs.ktor.client.okhttp)

            // Room (Android)
            implementation(libs.room.runtime)

            // Firebase Auth
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)

            // Google Credential Manager for One-Tap Sign-In
            implementation(libs.credentials)
            implementation(libs.credentials.playServices)
            implementation(libs.googleid)

            // Koin Android
            implementation(libs.koin.android)
        }
        if (isMacOs) {
            iosMain.dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.java)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
    }
}

// Make sure generated config is ready before any Kotlin compilation
tasks.configureEach {
    if (name.contains("compileKotlin", ignoreCase = true) ||
        name.contains("ksp", ignoreCase = true)
    ) {
        dependsOn(generateConfigTask)
    }
}

// ---------------------------------------------------------------------------
// KSP (Room compiler)
// ---------------------------------------------------------------------------
dependencies {
    add("kspAndroid", libs.room.compiler)
    if (isMacOs) {
        add("kspIosArm64", libs.room.compiler)
        add("kspIosSimulatorArm64", libs.room.compiler)
    }
    add("kspJvm", libs.room.compiler)
    add("androidRuntimeClasspath", libs.compose.uiTooling)
}

// ---------------------------------------------------------------------------
// Desktop
// ---------------------------------------------------------------------------
compose.desktop {
    application {
        mainClass = "es.sebas1705.axiomnode.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "es.sebas1705.axiomnode"
            packageVersion = "1.0.0"
        }
    }
}

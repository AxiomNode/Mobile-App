package es.sebas1705.axiomnode.config

/**
 * Platform-specific [AppConfig] provider.
 * Each platform reads the config values baked at build time.
 */
expect fun createAppConfig(): AppConfig


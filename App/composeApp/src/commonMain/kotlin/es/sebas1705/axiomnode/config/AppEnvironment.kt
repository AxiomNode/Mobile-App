package es.sebas1705.axiomnode.config

/**
 * Environment identifier matching the AxiomNode ecosystem convention.
 */
enum class AppEnvironment {
    DEV, STG, PROD;

    val isDev: Boolean get() = this == DEV
    val isStg: Boolean get() = this == STG
    val isProd: Boolean get() = this == PROD
    val isFirebaseEnabled: Boolean get() = this != DEV
}


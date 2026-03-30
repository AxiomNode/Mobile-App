package es.sebas1705.axiomnode

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.sebas1705.axiomnode.di.appModule
import es.sebas1705.axiomnode.di.dataModule
import es.sebas1705.axiomnode.di.domainModule
import es.sebas1705.axiomnode.di.platformModule
import es.sebas1705.axiomnode.di.viewModelModule
import es.sebas1705.axiomnode.di.networkModule
import es.sebas1705.axiomnode.presentation.country.CountryScreen
import es.sebas1705.axiomnode.ui.AppTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.KoinAppDeclaration

@Composable
@Suppress("ModifierTopMost")
fun App(
    modifier: Modifier = Modifier,
    koinAppDeclaration: KoinAppDeclaration? = null
) {
    KoinApplication(
        application = {
            koinAppDeclaration?.invoke(this)
            modules(appModule, dataModule, domainModule, viewModelModule, platformModule, networkModule)
        }
    ) {
        AppTheme {
            CountryScreen(modifier)
        }
    }
}
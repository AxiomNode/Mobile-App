package es.sebas1705.axiomnode.testsupport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Installs [dispatcher] as the coroutine `Main` dispatcher. Call [uninstall]
 * from `@AfterTest` to restore the original dispatcher.
 *
 * Using [UnconfinedTestDispatcher] by default keeps `viewModelScope.launch`
 * synchronous with respect to `runTest`, which is what most ViewModel tests
 * here expect.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) {
    fun install() {
        Dispatchers.setMain(dispatcher)
    }

    fun uninstall() {
        Dispatchers.resetMain()
    }
}

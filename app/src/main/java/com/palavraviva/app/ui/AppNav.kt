package com.palavraviva.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.palavraviva.app.ui.screens.HomeScreen
import com.palavraviva.app.ui.screens.PlanDetailScreen
import com.palavraviva.app.ui.screens.PlansScreen
import com.palavraviva.app.ui.screens.ProgressScreen
import com.palavraviva.app.ui.screens.ReaderScreen
import com.palavraviva.app.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val READER = "reader/{book}/{chapter}"
    const val PLANS = "plans"
    const val PLAN_DETAIL = "plan/{planId}"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    /**
     * Rota do leitor. [book] deve ser preferencialmente o bookId ("gn", "1sm");
     * nomes ("João", "1 Samuel") também funcionam — o ReaderViewModel aceita
     * ambos. O segmento é codificado para tolerar espaços/acentos/barras.
     */
    fun reader(book: String, chapter: Int) = "reader/${Uri.encode(book)}/$chapter"
    fun planDetail(planId: String) = "plan/$planId"
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selected: ImageVector,
)

private val tabs = listOf(
    Tab(Routes.HOME, "Início", Icons.Outlined.Home, Icons.Filled.Home),
    Tab(READER_TAB, "Bíblia", Icons.Outlined.AutoStories, Icons.Filled.AutoStories),
    Tab(Routes.PLANS, "Planos", Icons.Outlined.EventNote, Icons.Filled.EventNote),
    Tab(Routes.PROGRESS, "Perfil", Icons.Outlined.Person, Icons.Filled.Person),
)
// Aba Bíblia: rota PRÓPRIA, distinta de Routes.READER, para não colidir com
// "reader/{book}/{chapter}" — "reader/jo/3" casaria como destino literal e
// mostraria a barra inferior só nesse caso, e não nos demais livros.
private const val READER_TAB = "reader-tab"

@Composable
fun PalavraVivaApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.selected else tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(READER_TAB) { ReaderScreen("jo", 3, navController) }
            composable(Routes.READER) { entry ->
                val book = entry.arguments?.getString("book") ?: "gn"
                val chapter = entry.arguments?.getString("chapter")?.toIntOrNull() ?: 1
                ReaderScreen(book, chapter, navController)
            }
            composable(Routes.PLANS) { PlansScreen(navController) }
            composable(Routes.PLAN_DETAIL) { entry ->
                PlanDetailScreen(entry.arguments?.getString("planId") ?: "anual", navController)
            }
            composable(Routes.PROGRESS) { ProgressScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
        }
    }
}

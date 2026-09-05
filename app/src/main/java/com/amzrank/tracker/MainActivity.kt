package com.amzrank.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amzrank.tracker.ui.screens.DetailScreen
import com.amzrank.tracker.ui.screens.HomeScreen
import com.amzrank.tracker.ui.screens.WebVerifyScreen
import com.amzrank.tracker.ui.theme.AmzRankTrackerTheme
import com.amzrank.tracker.ui.viewmodel.DetailViewModel
import com.amzrank.tracker.ui.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 通知权限请求回调
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求 Android 13+ 运行时通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as AmzApplication
        val repository = app.repository
        val navigateTo = intent.getStringExtra("navigate_to")

        setContent {
            AmzRankTrackerTheme {
                val navController = rememberNavController()
                val startDestination = if (navigateTo == "web_verify") "web_verify" else "home"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("home") {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.Factory(repository)
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToDetail = { asin ->
                                navController.navigate("detail/$asin")
                            },
                            onNavigateToWebVerify = {
                                navController.navigate("web_verify")
                            }
                        )
                    }

                    composable(
                        route = "detail/{asin}",
                        arguments = listOf(navArgument("asin") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val asin = backStackEntry.arguments?.getString("asin") ?: ""
                        val detailViewModel: DetailViewModel = viewModel(
                            key = asin,
                            factory = DetailViewModel.Factory(asin, repository)
                        )
                        DetailScreen(
                            viewModel = detailViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("web_verify") {
                        WebVerifyScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

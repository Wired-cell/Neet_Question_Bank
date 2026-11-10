package com.example.questionbank.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.ui.screens.AddQuestionScreen
import com.example.questionbank.ui.screens.QuestionListScreen
import com.example.questionbank.ui.screens.SettingsScreen
import com.example.questionbank.ui.screens.TestScreen
import com.example.questionbank.ui.screens.VaultListScreen

@Composable
fun AppNavigation(vaultDao: VaultDao, questionDao: QuestionDao) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "vaultList"
    ) {
        composable("vaultList") {
            VaultListScreen(
                vaultDao = vaultDao,
                questionDao = questionDao,
                onNavigateToAddQuestion = { vaultId ->
                    navController.navigate("addQuestion/$vaultId")
                },
                onNavigateToQuestionList = { vaultId ->
                    navController.navigate("questionList/$vaultId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToTestMode = {
                    navController.navigate("testMode")
                }
            )
        }
        
        composable("questionList/{vaultId}") { backStackEntry ->
            val vaultId = backStackEntry.arguments?.getString("vaultId")?.toLongOrNull() ?: 0L
            QuestionListScreen(
                vaultId = vaultId,
                questionDao = questionDao,
                vaultDao = vaultDao,
                onNavigateToAddQuestion = { 
                    navController.navigate("addQuestion/$vaultId")
                },
                onNavigateToEditQuestion = { questionId ->
                    navController.navigate("addQuestion/$vaultId?questionId=$questionId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "addQuestion/{vaultId}?questionId={questionId}",
            arguments = listOf(
                navArgument("vaultId") { type = androidx.navigation.NavType.LongType },
                navArgument("questionId") { 
                    type = androidx.navigation.NavType.LongType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val vaultId = backStackEntry.arguments?.getLong("vaultId") ?: 0L
            val questionId = backStackEntry.arguments?.getLong("questionId")
            
            AddQuestionScreen(
                vaultId = vaultId,
                questionId = questionId,
                vaultDao = vaultDao,
                questionDao = questionDao,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("testMode") {
            TestScreen(
                vaultDao = vaultDao,
                questionDao = questionDao,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

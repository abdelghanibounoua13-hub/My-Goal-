package com.mygoal.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.Room

class MainActivity : ComponentActivity() {

    private val db: MyGoalDatabase by lazy {
        Room.databaseBuilder(applicationContext, MyGoalDatabase::class.java, "my_goal_cabinet.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("mygoal_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isAr by remember { mutableStateOf(prefs.getBoolean("is_ar", false)) }
            var isDark by remember { mutableStateOf(prefs.getBoolean("is_dark", false)) }
            var ruled by remember { mutableStateOf(prefs.getBoolean("ruled", true)) }

            MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides (if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr)
                    ) {
                        MyGoalApp(
                            dao = db.dao(),
                            isAr = isAr,
                            isDark = isDark,
                            ruled = ruled,
                            onToggleLanguage = {
                                isAr = !isAr
                                prefs.edit().putBoolean("is_ar", isAr).apply()
                            },
                            onToggleTheme = {
                                isDark = !isDark
                                prefs.edit().putBoolean("is_dark", isDark).apply()
                            },
                            onToggleRuled = {
                                ruled = !ruled
                                prefs.edit().putBoolean("ruled", ruled).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}

data class AppRoute(val name: String, val goalId: Long = -1L, val pageId: Long = -1L)

@Composable
fun MyGoalApp(
    dao: MyGoalDao,
    isAr: Boolean,
    isDark: Boolean,
    ruled: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleRuled: () -> Unit
) {
    val stack = remember { mutableStateListOf(AppRoute("home")) }
    val current = stack.last()

    fun go(name: String, goalId: Long = -1L, pageId: Long = -1L) {
        stack.add(AppRoute(name, goalId, pageId))
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { back() }

    when (current.name) {
        "form" -> GoalForm(
            dao = dao,
            isAr = isAr,
            goalId = current.goalId.takeIf { it >= 0L },
            onDone = { back() }
        )

        "notebook" -> NotebookView(
            dao = dao,
            isAr = isAr,
            goalId = current.goalId,
            onOpenPage = { pageId -> go("editor", current.goalId, pageId) },
            onEditGoal = { go("form", current.goalId) },
            onBack = { back() }
        )

        "editor" -> PageEditor(
            dao = dao,
            isAr = isAr,
            ruled = ruled,
            pageId = current.pageId,
            onBack = { back() }
        )

        "search" -> SearchView(
            dao = dao,
            isAr = isAr,
            onOpenGoal = { goalId -> go("notebook", goalId) },
            onOpenPage = { goalId, pageId -> go("editor", goalId, pageId) },
            onBack = { back() }
        )

        "completed", "archived" -> StatusView(
            dao = dao,
            isAr = isAr,
            archived = current.name == "archived",
            onOpenGoal = { goalId -> go("notebook", goalId) },
            onBack = { back() }
        )

        "settings" -> SettingsView(
            dao = dao,
            isAr = isAr,
            isDark = isDark,
            ruled = ruled,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme,
            onToggleRuled = onToggleRuled,
            onBack = { back() }
        )

        else -> CabinetHome(
            dao = dao,
            isAr = isAr,
            onOpenGoal = { goalId -> go("notebook", goalId) },
            onNewGoal = { go("form") },
            onSearch = { go("search") },
            onCompleted = { go("completed") },
            onArchived = { go("archived") },
            onSettings = { go("settings") }
        )
    }
}

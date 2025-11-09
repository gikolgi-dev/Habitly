/*package com.example.attempt3.previews

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.example.attempt3.Completion
import com.example.attempt3.ExpressiveDarkApp
import com.example.attempt3.ExpressiveMainScreen
import com.example.attempt3.Habit
import com.example.attempt3.HabitDao
import com.example.attempt3.HabitViewModel
import com.example.attempt3.HabitWithCompletions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PreviewHabitDao : HabitDao {
    override fun getHabitsWithCompletions(): Flow<List<HabitWithCompletions>> = flowOf(
        listOf(
            HabitWithCompletions(
                Habit("1", "Morning Jog", "Run 5k every morning", "run", Color.Red.toArgb(), false, 0, "", false, null),
                completions = listOf()
            ),
            HabitWithCompletions(
                Habit("2", "Read a book", "Read 20 pages of a book", "book", Color.Blue.toArgb(), false, 1, "", false, null),
                completions = listOf()
            )
        )
    )

    override fun getArchivedHabitsWithCompletions(): Flow<List<HabitWithCompletions>> = flowOf(emptyList())
    override fun getArchivedHabits(): Flow<List<Habit>> {
        TODO("Not yet implemented")
    }

    override fun getCompletionsForHabit(habitId: String): Flow<List<Completion>> = flowOf(emptyList())
    override suspend fun countCompletionsForHabitOnDay(
        habitId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Int {
        TODO("Not yet implemented")
    }

    override suspend fun insertHabit(habit: Habit) {}
    override suspend fun updateHabit(habit: Habit) {}
    override suspend fun updateHabits(habits: List<Habit>) {}
    override suspend fun deleteHabit(habit: Habit) {}
    override fun getCompletionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Completion>> = flowOf(emptyList())
    override suspend fun insertCompletion(completion: Completion) {}
    override fun getAllHabits(): Flow<List<Habit>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCompletionsForHabitOnDay(habitId: String, startOfDay: Long, endOfDay: Long) {}
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, backgroundColor = 0x0B0B0E)
@Composable
fun PreviewExpressiveMainScreen() {
    val viewModel = HabitViewModel(PreviewHabitDao())
    ExpressiveDarkApp(viewModel = viewModel, habitDao = PreviewHabitDao()) { ExpressiveMainScreen(viewModel = viewModel, habitDao = PreviewHabitDao()) }
}*/

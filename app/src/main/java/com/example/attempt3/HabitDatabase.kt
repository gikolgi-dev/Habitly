package com.example.attempt3

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: Int,
    val archived: Boolean,
    val orderIndex: Int,
    val createdAt: String,
    val isInverse: Boolean,
    val emoji: String?,
    val completionsPerInterval: Int = 1,
    val intervalValue: Int = 1,
    val intervalUnit: String = "day"
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Completion(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: Long,
    val timezoneOffsetInMinutes: Int,
    val amountOfCompletions: Int
)

@Dao
interface HabitDao {
    @Insert
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Update
    suspend fun updateHabits(habits: List<Habit>)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Insert
    suspend fun insertCompletion(completion: Completion)

    @Query("SELECT * FROM habit WHERE archived = 0 ORDER BY orderIndex")
    fun getAllHabits(): Flow<List<Habit>>

    @Transaction
    @Query("SELECT * FROM habit WHERE archived = 0 ORDER BY orderIndex")
    fun getHabitsWithCompletions(): Flow<List<HabitWithCompletions>>

    @Transaction
    @Query("SELECT * FROM habit WHERE archived = 1 ORDER BY orderIndex")
    fun getArchivedHabitsWithCompletions(): Flow<List<HabitWithCompletions>>


    @Query("SELECT * FROM habit WHERE archived = 1 ORDER BY orderIndex")
    fun getArchivedHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM completion WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: String): Flow<List<Completion>>



    @Query("SELECT COUNT(*) FROM completion WHERE habitId = :habitId AND date >= :startOfDay AND date < :endOfDay")
    suspend fun countCompletionsForHabitOnDay(habitId: String, startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT * FROM completion WHERE date >= :startOfDay AND date < :endOfDay")
    fun getCompletionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Completion>>

    @Query("DELETE FROM completion WHERE habitId = :habitId AND date >= :startOfDay AND date < :endOfDay")
    suspend fun deleteCompletionsForHabitOnDay(habitId: String, startOfDay: Long, endOfDay: Long)
}

@Database(entities = [Habit::class, Completion::class], version = 8)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Completion ADD COLUMN timezoneOffsetInMinutes INTEGER NOT NULL DEFAULT 0")
    }
}
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Completion ADD COLUMN amountOfCompletions INTEGER NOT NULL DEFAULT 1")
    }
}
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Habit ADD COLUMN completionsPerInterval INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE Habit ADD COLUMN intervalValue INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE Habit ADD COLUMN intervalUnit TEXT NOT NULL DEFAULT 'day'")
    }
}
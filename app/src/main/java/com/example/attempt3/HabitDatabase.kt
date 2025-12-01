package com.example.attempt3

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
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
    val intervalUnit: String = "day",
    val notificationsEnabled: Boolean = false,
    val notificationTime: String? = null,
    val notificationDays: String? = null
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<Habit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletions(completions: List<Completion>)

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
    
    @Query("SELECT * FROM habit WHERE id = :habitId")
    suspend fun getHabit(habitId: String): Habit?

    @Transaction
    @Query("SELECT * FROM habit ORDER BY orderIndex")
    suspend fun getAllHabitsWithCompletionsSnapshot(): List<HabitWithCompletions>

    @Transaction
    suspend fun clearAllTables() {
        clearHabits()
        clearCompletions()
    }

    @Query("DELETE FROM habit")
    suspend fun clearHabits()

    @Query("DELETE FROM completion")
    suspend fun clearCompletions()
}

@Database(entities = [Habit::class, Completion::class], version = 11)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_database"
                ).addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build()
                INSTANCE = instance
                instance
            }
        }
    }
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
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create a new table with the desired schema
        database.execSQL("""
            CREATE TABLE Habit_new (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                icon TEXT NOT NULL,
                color INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                orderIndex INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                isInverse INTEGER NOT NULL,
                emoji TEXT,
                completionsPerInterval INTEGER NOT NULL,
                intervalUnit TEXT NOT NULL,
                PRIMARY KEY(id)
            )
        """.trimIndent())

        // Copy the data from the old table to the new table
        database.execSQL("""
            INSERT INTO Habit_new (id, name, description, icon, color, archived, orderIndex, createdAt, isInverse, emoji, completionsPerInterval, intervalUnit)
            SELECT id, name, description, icon, color, archived, orderIndex, createdAt, isInverse, emoji, completionsPerInterval, intervalUnit FROM Habit
        """.trimIndent())

        // Drop the old table
        database.execSQL("DROP TABLE Habit")

        // Rename the new table to the original table name
        database.execSQL("ALTER TABLE Habit_new RENAME TO Habit")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Habit ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE Habit ADD COLUMN notificationTime TEXT")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Habit ADD COLUMN notificationDays TEXT")
    }
}

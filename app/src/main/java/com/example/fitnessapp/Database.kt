package com.example.fitnessapp

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// User Entity - Updated to match ProfileFragment requirements
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1,
    val fullName: String,
    val email: String,
    val weight: Double,
    val height: Int,
    val stepGoal: Int,
    val waterGoal: Double,
    val usePhoneSensors: Boolean
)

// User DAO - Updated to return Flow for real-time UI updates
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = 1 LIMIT 1")
    fun getUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User): Int

    @Query("DELETE FROM users")
    suspend fun deleteUser(): Int
}

// App Database
@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

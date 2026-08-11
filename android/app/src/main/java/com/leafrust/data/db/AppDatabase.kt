package com.leafrust.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [InspectionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspections ADD COLUMN treatment TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inspections ADD COLUMN plantKind TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "leafrust.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

package es.sebas1705.axiomnode.di

import android.content.Context
import androidx.room.Room
import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

actual fun provideDatabase(): AxiomNodeDatabase {
    val context: Context = GlobalContext.get().get()
    return Room.databaseBuilder(
        context.applicationContext,
        AxiomNodeDatabase::class.java,
        "axiomnode.db",
    )
        .fallbackToDestructiveMigration()
        .build()
}


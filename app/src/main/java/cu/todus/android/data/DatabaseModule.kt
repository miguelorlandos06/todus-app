package cu.todus.android.data

import android.content.Context
import androidx.room.Room
import cu.todus.android.data.db.TodusDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TodusDatabase =
        Room.databaseBuilder(context, TodusDatabase::class.java, "todus.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideContactDao(db: TodusDatabase) = db.contactDao()
    @Provides fun provideChatDao(db: TodusDatabase) = db.chatDao()
    @Provides fun provideMessageDao(db: TodusDatabase) = db.messageDao()
}

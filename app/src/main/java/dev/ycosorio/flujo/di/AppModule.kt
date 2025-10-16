package dev.ycosorio.flujo.di

import com.google.firebase.firestore.FirebaseFirestore
import dev.ycosorio.flujo.data.repository.UserRepositoryImpl
import dev.ycosorio.flujo.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository {
        // Hilt sabe cómo crear 'firestore' gracias a la función de arriba,
        // así que la inyecta aquí automáticamente.
        return UserRepositoryImpl(firestore)
    }
}
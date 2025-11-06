package dev.ycosorio.flujo.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dev.ycosorio.flujo.data.repository.UserRepositoryImpl
import dev.ycosorio.flujo.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dev.ycosorio.flujo.data.repository.InventoryRepositoryImpl
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.data.repository.DocumentRepositoryImpl
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ycosorio.flujo.BuildConfig
import dev.ycosorio.flujo.data.preferences.UserPreferencesRepository
import dev.ycosorio.flujo.data.repository.AuthRepositoryImpl
import dev.ycosorio.flujo.domain.repository.AuthRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

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

    @Provides
    @Singleton
    fun provideInventoryRepository(firestore: FirebaseFirestore): InventoryRepository {
        return InventoryRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): DocumentRepository {

        return DocumentRepositoryImpl(firestore, storage)
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        val functions = FirebaseFunctions.getInstance()

        // Solo para emulador local
        if (BuildConfig.DEBUG) {
            functions.useEmulator("10.0.2.2", 5001)
        }

        return functions
    }
}
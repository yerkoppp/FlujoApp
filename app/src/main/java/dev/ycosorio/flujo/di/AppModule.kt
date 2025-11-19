package dev.ycosorio.flujo.di

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ycosorio.flujo.data.preferences.UserPreferencesRepository
import dev.ycosorio.flujo.data.repository.AuthRepositoryImpl
import dev.ycosorio.flujo.data.repository.DocumentRepositoryImpl
import dev.ycosorio.flujo.data.repository.ExpenseRepositoryImpl
import dev.ycosorio.flujo.data.repository.InventoryRepositoryImpl
import dev.ycosorio.flujo.data.repository.MessageRepositoryImpl
import dev.ycosorio.flujo.data.repository.NotificationRepositoryImpl
import dev.ycosorio.flujo.data.repository.UserRepositoryImpl
import dev.ycosorio.flujo.data.repository.VehicleRepositoryImpl
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.domain.repository.DocumentRepository
import dev.ycosorio.flujo.domain.repository.ExpenseRepository
import dev.ycosorio.flujo.domain.repository.InventoryRepository
import dev.ycosorio.flujo.domain.repository.MessageRepository
import dev.ycosorio.flujo.domain.repository.NotificationRepository
import dev.ycosorio.flujo.domain.repository.UserRepository
import dev.ycosorio.flujo.domain.repository.VehicleRepository
import javax.inject.Singleton

/**
 * Módulo de Hilt que proporciona las dependencias de la aplicación.
 * Aquí se definen los proveedores para los repositorios y servicios de Firebase.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Proporciona una instancia de UserPreferencesRepository. */
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    /** Proporciona una instancia de FirebaseFirestore. */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    /** Proporciona una instancia de UserRepository. */
    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository {
        return UserRepositoryImpl(firestore)
    }

    /** Proporciona una instancia de InventoryRepository. */
    @Provides
    @Singleton
    fun provideInventoryRepository(firestore: FirebaseFirestore): InventoryRepository {
        return InventoryRepositoryImpl(firestore)
    }

    /** Proporciona una instancia de DocumentRepository. */
    @Provides
    @Singleton
    fun provideDocumentRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): DocumentRepository {

        return DocumentRepositoryImpl(firestore, storage)
    }

    /** Proporciona una instancia de FirebaseStorage. */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    /** Proporciona una instancia de FirebaseAuth. */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /** Proporciona una instancia de FirebaseMessaging. */
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

    /** Proporciona una instancia de AuthRepository. */
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
       // firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth)
    }

    /** Proporciona una instancia de VehicleRepository. */
    @Provides
    @Singleton
    fun provideVehicleRepository(firestore: FirebaseFirestore): VehicleRepository =
        VehicleRepositoryImpl(firestore)

    /** Proporciona una instancia de FirebaseFunctions. */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        val functions = Firebase.functions("southamerica-west1")
        return functions
    }

    /** Proporciona una instancia de MessageRepository. */
    @Provides
    @Singleton
    fun provideMessageRepository(firestore: FirebaseFirestore): MessageRepository {
        val functions = Firebase.functions("southamerica-west1")
        return MessageRepositoryImpl(firestore, functions)
    }

    /** Proporciona una instancia de ExpenseRepository. */
    @Provides
    @Singleton
    fun provideExpenseRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): ExpenseRepository {
        return ExpenseRepositoryImpl(firestore, storage)
    }

    /** Proporciona una instancia de NotificationRepository. */
    @Provides
    @Singleton
    fun provideNotificationRepository(firestore: FirebaseFirestore): NotificationRepository =
        NotificationRepositoryImpl(firestore)

}
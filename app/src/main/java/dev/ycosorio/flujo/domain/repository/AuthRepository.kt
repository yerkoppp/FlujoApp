package dev.ycosorio.flujo.domain.repository

import dev.ycosorio.flujo.domain.model.AuthUser
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    suspend fun signInWithGoogle(idToken: String): Resource<AuthUser>
    suspend fun signOut(): Resource<Unit>
    fun getCurrentUser(): AuthUser?
}
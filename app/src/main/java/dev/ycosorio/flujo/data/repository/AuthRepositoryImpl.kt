package dev.ycosorio.flujo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dev.ycosorio.flujo.domain.model.AuthUser
import dev.ycosorio.flujo.domain.repository.AuthRepository
import dev.ycosorio.flujo.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementación del repositorio de autenticación utilizando Firebase Authentication.
 *
 * @property firebaseAuth La instancia de FirebaseAuth para gestionar la autenticación.
 */
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    /**
     * Flujo que emite el usuario autenticado actual o null si no hay ningún usuario autenticado.
     * Cada vez que el estado de autenticación cambia, se emite el nuevo usuario.
     */
    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            Timber.d("Estado de auth cambió: ${user?.email}")
            trySend(auth.currentUser?.toAuthUser())
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Inicia sesión con Google utilizando el token de ID proporcionado.
     *
     * @param idToken El token de ID de Google para autenticar al usuario.
     * @return Un recurso que contiene el usuario autenticado o un error en caso de fallo.
     */
    override suspend fun signInWithGoogle(idToken: String): Resource<AuthUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Resource.Success(firebaseUser.toAuthUser())
            } else {
                Resource.Error("Error al iniciar sesión")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error desconocido al iniciar sesión")
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     *
     * @return Un recurso que indica el éxito o el error de la operación.
     */
    override suspend fun signOut(): Resource<Unit> {
        return try {
            firebaseAuth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al cerrar sesión")
        }
    }

    /**
     * Obtiene el usuario autenticado actual.
     *
     * @return El usuario autenticado o null si no hay ningún usuario autenticado.
     */
    override fun getCurrentUser(): AuthUser? {
        return firebaseAuth.currentUser?.toAuthUser()
    }
}

/**
 * Convierte un objeto FirebaseUser a un objeto AuthUser.
 *
 * @receiver FirebaseUser El usuario de Firebase a convertir.
 * @return AuthUser El usuario convertido.
 */
private fun com.google.firebase.auth.FirebaseUser.toAuthUser(): AuthUser {
    return AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )
}
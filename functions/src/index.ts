// Archivo: functions/src/index.ts

// Importaciones Modulares (Estilo v4/v2)
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {setGlobalOptions} from "firebase-functions/v2"; // Para definir la región global
import * as admin from "firebase-admin";

admin.initializeApp();

// Define la región globalmente para todas las funciones
setGlobalOptions({region: "southamerica-west1"});

// --- FUNCIONES DE VALIDACIÓN ---

/**
 * Valida que un email tenga formato correcto
 */
function isValidEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * Valida que un string tenga longitud válida
 */
function isValidStringLength(
  str: string,
  minLength: number,
  maxLength: number
): boolean {
  return str.length >= minLength && str.length <= maxLength;
}

/**
 * Valida que una fecha tenga formato válido (YYYY-MM-DD)
 */
function isValidDateFormat(dateStr: string): boolean {
  const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!dateRegex.test(dateStr)) return false;

  const date = new Date(dateStr);
  return !isNaN(date.getTime());
}

/**
 * Sanitiza un mensaje de error para evitar exponer información sensible
 */
function sanitizeErrorMessage(error: any): string {
  // Si es un HttpsError, retornar su mensaje (ya es seguro)
  if (error instanceof HttpsError) {
    return error.message;
  }

  // Para otros errores, retornar mensaje genérico
  // y loguear el error real para debugging
  console.error("Error interno:", error);
  return "Ocurrió un error interno. Por favor, contacta al soporte.";
}

// --- INTERFACES DE DATOS ---

/**
 * Define la estructura de los datos que esperamos
 * recibir desde la app cliente (Flujo).
 */
interface CreateWorkerData {
  email: string;
  name: string;
  position: string;
  area: string;
  contractStartDate: string; // Se espera un string de fecha (ej: "2025-10-31")
}

/**
 * Define la estructura de nuestro documento de usuario
 * en la colección 'users' de Firestore.
 */
interface AppUser {
  uid: string;
  name: string;
  email: string;
  role: "TRABAJADOR" | "ADMINISTRADOR";
  position: string;
  area: string;
  contractStartDate: admin.firestore.Timestamp;
  contractEndDate: admin.firestore.Timestamp | null;
  phoneNumber: string | null;
  photoUrl: string | null;
  assignedVehicleId: string | null;
  assignedPhoneId: string | null;
  assignedPcId: string | null;
}

/**
 * Define la estructura de los datos en la colección 'invitations'.
 * El ID del documento será el email del invitado.
 */
interface InvitationData {
  name: string;
  position: string;
  area: string;
  role: "TRABAJADOR" | "ADMINISTRADOR";
  contractStartDate: admin.firestore.Timestamp;
}

// --- FUNCIÓN 1: createWorker ---

export const createWorker = onCall(async (request) => {
  // 1. Verificar autenticación
  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "Debes estar autenticado para realizar esta acción"
    );
  }

  // 2. Verificar que quien llama es admin
  const callerUid = request.auth.uid;
  const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
  const callerData = callerDoc.data();

  if (!callerDoc.exists || callerData?.role !== "ADMINISTRADOR") {
    throw new HttpsError(
      "permission-denied",
      "Solo los administradores pueden crear nuevos trabajadores"
    );
  }

  // 3. Validar datos recibidos
  const data = request.data as CreateWorkerData;
  const {email, name, position, area, contractStartDate} = data;

  // Validar que existan todos los campos obligatorios
  if (!email || !name || !position || !area || !contractStartDate) {
    throw new HttpsError(
      "invalid-argument",
      "Faltan datos obligatorios: email, name, position, area y contractStartDate"
    );
  }

  // Validar formato de email
  const normalizedEmail = email.toLowerCase().trim();
  if (!isValidEmail(normalizedEmail)) {
    throw new HttpsError(
      "invalid-argument",
      "El formato del email no es válido"
    );
  }

  // Validar longitud de campos
  if (!isValidStringLength(name, 2, 100)) {
    throw new HttpsError(
      "invalid-argument",
      "El nombre debe tener entre 2 y 100 caracteres"
    );
  }

  if (!isValidStringLength(position, 2, 100)) {
    throw new HttpsError(
      "invalid-argument",
      "El cargo debe tener entre 2 y 100 caracteres"
    );
  }

  if (!isValidStringLength(area, 2, 100)) {
    throw new HttpsError(
      "invalid-argument",
      "El área debe tener entre 2 y 100 caracteres"
    );
  }

  // Validar formato de fecha
  if (!isValidDateFormat(contractStartDate)) {
    throw new HttpsError(
      "invalid-argument",
      "La fecha de contrato debe tener formato YYYY-MM-DD"
    );
  }

  // 4. Verificar si el email ya existe en 'users' O en 'invitations'
  try {
    const usersQuery = await admin.firestore().collection("users")
      .where("email", "==", normalizedEmail).get();
    if (!usersQuery.empty) {
      throw new HttpsError(
        "already-exists",
        "Ya existe un trabajador provisionado con este email"
      );
    }
    const invitationDoc = await admin.firestore().collection("invitations")
      .doc(normalizedEmail).get();
    if (invitationDoc.exists) {
      throw new HttpsError(
        "already-exists",
        "Ya existe una invitación pendiente para este email"
      );
    }
  } catch (error: any) {
    if (error instanceof HttpsError) throw error;
    console.error("Error al verificar email:", error);
    throw new HttpsError("internal", sanitizeErrorMessage(error));
  }

  // Ya no se crea el usuario en Authentication.
  try {
    const newInvitation: InvitationData = {
      name: name,
      position: position,
      area: area,
      role: "TRABAJADOR", // La función solo crea trabajadores
      contractStartDate: admin.firestore.Timestamp.fromDate(
        new Date(contractStartDate)
      ),
    };
    await admin.firestore()
      .collection("invitations")
      .doc(normalizedEmail)
      .set(newInvitation);

    console.log("Invitación creada para:", normalizedEmail);

    return {
      success: true,
      message: "Invitación creada exitosamente",
    };
  } catch (error: any) {
    console.error("Error al crear invitación:", error);
    throw new HttpsError("internal", sanitizeErrorMessage(error));
  }
});

// --- FUNCIÓN 2 (NUEVA): provisionUserAccount ---

/**
 * Se llama esta función desde la app cliente JUSTO DESPUÉS
 * de que un usuario inicie sesión por primera vez con Google
 * y la app descubra que no tiene un documento en 'users'.
 */
export const provisionUserAccount = onCall(async (request) => {
  // 1. Verificar que el usuario que llama está autenticado
  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "Debes estar autenticado para provisionar tu cuenta"
    );
  }

  // 2. Obtener datos del usuario (desde su token de Google)
  const uid = request.auth.uid;
  const email = request.auth.token.email;
  const googlePhotoUrl = request.auth.token.picture || null;

  if (!email) {
    throw new HttpsError(
      "invalid-argument",
      "Tu cuenta de Google no tiene un email asociado."
    );
  }

  const normalizedEmail = email.toLowerCase().trim();

  // 3. Definir referencias
  const userDocRef = admin.firestore().collection("users").doc(uid);
  const invitationDocRef = admin.firestore().collection("invitations").doc(normalizedEmail);

  // 4. Ejecutar una transacción ATÓMICA
  // Esto asegura que o se hacen ambas cosas (crear usuario y borrar inv.) o no se hace nada.
  try {
    await admin.firestore().runTransaction(async (t) => {
      // Leer los documentos DENTRO de la transacción
      const userDoc = await t.get(userDocRef);
      if (userDoc.exists) {
        // El usuario ya fue provisionado en un intento anterior. No hacer nada.
        console.log("El usuario ya existe, no se requiere provisión:", uid);
        return;
      }

      const invitationDoc = await t.get(invitationDocRef);
      if (!invitationDoc.exists) {
        throw new HttpsError(
          "not-found",
          "No se encontró una invitación para tu email. Contacta al administrador."
        );
      }

      const invitationData = invitationDoc.data() as InvitationData;

      // 5. Preparar el nuevo documento de usuario
      const newUserDocument: AppUser = {
        uid: uid,
        name: invitationData.name, // Usamos el nombre oficial de la invitación
        email: normalizedEmail,
        role: invitationData.role,
        position: invitationData.position,
        area: invitationData.area,
        contractStartDate: invitationData.contractStartDate,
        contractEndDate: null,
        phoneNumber: null,
        photoUrl: googlePhotoUrl, // Usamos la foto de perfil de Google
        assignedVehicleId: null,
        assignedPhoneId: null,
        assignedPcId: null,
      };

      // 6. Ejecutar las operaciones DENTRO de la transacción
      t.set(userDocRef, newUserDocument); // Crear el documento 'users'
      t.delete(invitationDocRef); // Eliminar la invitación
    });

    console.log("Cuenta provisionada exitosamente para:", uid, normalizedEmail);
    return {
      success: true,
      message: "Tu cuenta ha sido activada exitosamente.",
    };
  } catch (error: any) {
    console.error("Error en la transacción de provisión:", error);
    if (error instanceof HttpsError) {
      throw error; // Relanzar errores Https (ej: 'not-found')
    }
    throw new HttpsError("internal", sanitizeErrorMessage(error));
  }
});

/**
 * Define los datos esperados para eliminar un trabajador.
 */
interface DeleteWorkerData {
  userId: string;
}

export const deleteWorker = onCall(async (request) => { // CAMBIO: onCall y request
  // 1. Verificar autenticación (CAMBIO: request.auth)
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes estar autenticado");
  }

  // 2. Verificar que quien llama es admin (CAMBIO: request.auth.uid)
  const callerUid = request.auth.uid;
  const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
  const callerData = callerDoc.data();

  if (!callerDoc.exists || callerData?.role !== "ADMINISTRADOR") {
    throw new HttpsError("permission-denied", "Solo los administradores pueden eliminar usuarios");
  }

  // 3. Obtener el UID del usuario a eliminar (CAMBIO: request.data)
  const data = request.data as DeleteWorkerData;
  const {userId} = data;

  if (!userId || typeof userId !== "string") {
    throw new HttpsError("invalid-argument", "El userId es obligatorio y debe ser un string");
  }

  // Validar que el userId no esté vacío después de trim
  if (userId.trim().length === 0) {
    throw new HttpsError("invalid-argument", "El userId no puede estar vacío");
  }

  // Validar longitud razonable del UID (Firebase UIDs tienen ~28 caracteres)
  if (!isValidStringLength(userId, 10, 128)) {
    throw new HttpsError("invalid-argument", "El userId tiene un formato inválido");
  }

  try {
    const pendingRequests = await admin.firestore()
      .collection("material_requests")
      .where("workerId", "==", userId)
      .where("status", "in", ["PENDIENTE", "APROBADO"])
      .limit(1)
      .get();

    if (!pendingRequests.empty) {
      throw new HttpsError(
        "failed-precondition",
        "No se puede eliminar: el trabajador tiene solicitudes pendientes o aprobadas"
      );
    }

    // ✅ Verificar documentos pendientes
    const pendingDocs = await admin.firestore()
      .collection("document_assignments")
      .where("workerId", "==", userId)
      .where("status", "==", "PENDIENTE")
      .limit(1)
      .get();

    if (!pendingDocs.empty) {
      throw new HttpsError(
        "failed-precondition",
        "No se puede eliminar: el trabajador tiene documentos pendientes de firma"
      );
    }
    // 4. Eliminar de Authentication (Importante: esto elimina su login de Google)
    await admin.auth().deleteUser(userId);
    console.log("Usuario eliminado de Auth:", userId);

    // 5. Eliminar de Firestore
    await admin.firestore().collection("users").doc(userId).delete();
    console.log("Documento eliminado de Firestore:", userId);

    return {success: true, message: "Usuario eliminado exitosamente"};
  } catch (error: any) {
    console.error("Error al eliminar trabajador:", error);

    if (error instanceof HttpsError) {
      throw error;
    }

    // Manejo de error si el usuario no existe
    if (error.code === "auth/user-not-found") {
      throw new HttpsError("not-found", "El usuario a eliminar no existe en Authentication");
    }

    throw new HttpsError("internal", sanitizeErrorMessage(error));
  }
});

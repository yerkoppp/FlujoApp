// Archivo: functions/src/index.ts

// Importaciones Modulares (Estilo v4/v2)
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {setGlobalOptions} from "firebase-functions/v2"; // Para definir la región global
import * as admin from "firebase-admin";

admin.initializeApp();

// Define la región globalmente para todas las funciones
setGlobalOptions({region: "southamerica-west1"});

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

export const createWorker = onCall(async (request) => {
  // 1. Verificar autenticación (CAMBIO: request.auth)
  if (!request.auth) {
    throw new HttpsError( // CAMBIO: HttpsError (sin 'functions.https')
      "unauthenticated",
      "Debes estar autenticado para realizar esta acción"
    );
  }

  // 2. Verificar que quien llama es admin (CAMBIO: request.auth.uid)
  const callerUid = request.auth.uid;
  const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
  const callerData = callerDoc.data();

  if (!callerDoc.exists || callerData?.role !== "ADMINISTRADOR") {
    throw new HttpsError(
      "permission-denied",
      "Solo los administradores pueden crear nuevos trabajadores"
    );
  }

  // 3. Validar datos recibidos (CAMBIO: request.data)
  // TypeScript no conoce el tipo de request.data, así que lo "casteamos"
  const data = request.data as CreateWorkerData;
  const {email, name, position, area, contractStartDate} = data;

  if (!email || !name || !position || !area || !contractStartDate) {
    throw new HttpsError(
      "invalid-argument",
      "Faltan datos obligatorios"
    );
  }

  // 4. Verificar si el email ya existe (Manejo de error corregido)
  try {
    await admin.auth().getUserByEmail(email);
    // Si la promesa se resuelve, el usuario SÍ existe. Lanzamos error.
    throw new HttpsError(
      "already-exists",
      "Ya existe un usuario con este email"
    );
  } catch (error: any) {
    // Si el error es el que lanzamos arriba, lo relanza.
    if (error instanceof HttpsError) {
      throw error;
    }
    // Esperamos que el error sea 'auth/user-not-found'.
    if (error.code !== "auth/user-not-found") {
      throw error; // Relanza cualquier otro error inesperado
    }
    // Si es 'auth/user-not-found', está bien. Continuamos.
  }

  // 5. y 6. Crear usuario en Auth y documento en Firestore
  try {
    const userRecord = await admin.auth().createUser({
      email: email.toLowerCase().trim(),
      emailVerified: false,
      disabled: false,
    });

    console.log("Usuario creado en Auth:", userRecord.uid);

    // Usamos la interfaz AppUser para asegurar que los datos son correctos
    const newUserDocument: AppUser = {
      uid: userRecord.uid,
      name: name,
      email: email.toLowerCase().trim(),
      role: "TRABAJADOR",
      position: position,
      area: area,
      contractStartDate: admin.firestore.Timestamp.fromDate(
        new Date(contractStartDate) // Convertimos el string a Fecha
      ),
      contractEndDate: null,
      phoneNumber: null,
      photoUrl: null,
      assignedVehicleId: null,
      assignedPhoneId: null,
      assignedPcId: null,
    };

    await admin.firestore()
      .collection("users")
      .doc(userRecord.uid)
      .set(newUserDocument);

    console.log("Documento creado en Firestore:", userRecord.uid);

    return {
      success: true,
      uid: userRecord.uid,
      message: "Trabajador creado exitosamente",
    };
  } catch (error: any) {
    // Si falla la creación en Auth o Firestore.
    console.error("Error en pasos 5 o 6:", error);
    throw new HttpsError(
      "internal",
      error.message || "Error interno al crear el trabajador"
    );
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

  if (!userId) {
    throw new HttpsError("invalid-argument", "Falta el userId");
  }

  try {
    // 4. Eliminar de Authentication
    await admin.auth().deleteUser(userId);
    console.log("Usuario eliminado de Auth:", userId);

    // 5. Eliminar de Firestore
    await admin.firestore().collection("users").doc(userId).delete();
    console.log("Documento eliminado de Firestore:", userId);

    return {success: true, message: "Usuario eliminado exitosamente"};
  } catch (error: any) {
    console.error("Error al eliminar trabajador:", error);

    // Manejo de error si el usuario no existe
    if (error.code === "auth/user-not-found") {
      throw new HttpsError("not-found", "El usuario a eliminar no existe en Authentication");
    }

    throw new HttpsError("internal", error.message || "Error interno al eliminar usuario");
  }
});

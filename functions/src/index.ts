// Archivo: functions/src/index.ts

import *import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
admin.initializeApp();

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
  role: 'TRABAJADOR' | 'ADMINISTRADOR'; // Usamos tipos literales
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

export const createWorker = functions.https.onCall(async (data: CreateWorkerData, context) => {

  // 1. Verificar autenticación
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Debes estar autenticado para realizar esta acción'
    );
  }

  // 2. Verificar que quien llama es admin
  const callerUid = context.auth.uid;
  const callerDoc = await admin.firestore().collection('users').doc(callerUid).get();
  const callerData = callerDoc.data(); // TS sabe que esto puede ser undefined

  if (!callerDoc.exists || callerData?.role !== 'ADMINISTRADOR') {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Solo los administradores pueden crear nuevos trabajadores'
    );
  }

  // 3. Validar datos recibidos (usando la interfaz)
  const { email, name, position, area, contractStartDate } = data;
  if (!email || !name || !position || !area || !contractStartDate) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Faltan datos obligatorios'
    );
  }

  // 4. Verificar si el email ya existe (Manejo de error corregido)
  try {
    await admin.auth().getUserByEmail(email);
    // Si la promesa se resuelve, el usuario SÍ existe. Lanzamos error.
    throw new functions.https.HttpsError(
      'already-exists',
      'Ya existe un usuario con este email'
    );
  } catch (error: any) {
    // Esperamos que el error sea 'auth/user-not-found'.
    // Si NO lo es, relanzamos el error (que podría ser el 'already-exists' de arriba).
    if (error.code !== 'auth/user-not-found') {
      throw error;
    }
    // Si es 'auth/user-not-found', está bien. Continuamos.
  }

  // 5. y 6. Crear usuario en Auth y documento en Firestore
  // Lo ponemos en su propio Try/Catch para errores de CREACIÓN
  try {
    const userRecord = await admin.auth().createUser({
      email: email.toLowerCase().trim(),
      emailVerified: false,
      disabled: false,
    });

    console.log('Usuario creado en Auth:', userRecord.uid);

    // Usamos la interfaz AppUser para asegurar que los datos son correctos
    const newUserDocument: AppUser = {
      uid: userRecord.uid,
      name: name,
      email: email.toLowerCase().trim(),
      role: 'TRABAJADOR',
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
      assignedPcId: null
    };

    await admin.firestore()
      .collection('users')
      .doc(userRecord.uid)
      .set(newUserDocument);

    console.log('Documento creado en Firestore:', userRecord.uid);

    return {
      success: true,
      uid: userRecord.uid,
      message: 'Trabajador creado exitosamente'
    };

  } catch (error: any) {
    // Si falla la creación en Auth o Firestore.
    console.error('Error en pasos 5 o 6:', error);
    throw new functions.https.HttpsError(
      'internal',
      error.message || 'Error interno al crear el trabajador'
    );
  }
});
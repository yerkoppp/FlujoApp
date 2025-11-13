# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Reglas para FirebaseUI (Específicas para las clases no encontradas)
-keep class com.firebase.ui.auth.data.model.** { *; }
-keep class com.firebase.ui.auth.** { *; }

# Reglas generales recomendadas para FirebaseUI
-keepnames class com.google.android.gms.common.api.CommonStatusCodes
-keepnames class com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
-keepnames class com.google.firebase.auth.FirebaseAuthException
-keepnames class com.google.firebase.firestore.FirebaseFirestoreException
-keepnames class com.google.firebase.storage.StorageException

# ====================================
# FIREBASE FIRESTORE - Modelos
# ====================================
# Mantener todos los modelos de dominio sin ofuscar
-keep class dev.ycosorio.flujo.domain.model.** { *; }
-keepclassmembers class dev.ycosorio.flujo.domain.model.** { *; }

# Mantener constructores sin argumentos
-keepclassmembers class * {
    public <init>();
}

# Mantener atributos para serialización
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Mantener enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ====================================
# FIREBASE STORAGE
# ====================================
-keep class com.google.firebase.storage.** { *; }

# ====================================
# KOTLIN
# ====================================
# Mantener clases de datos de Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}

# Mantener constructores por defecto para data classes
-keepclassmembers class * {
    public <init>(...);
}

# ====================================
# HILT/DAGGER
# ====================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ====================================
# COIL (Carga de imágenes)
# ====================================
-keep class coil.** { *; }

# ====================================
# MODELOS ESPECÍFICOS DE RENDICIÓN
# ====================================
# Asegurarse de que los nuevos modelos no sean ofuscados
-keep class dev.ycosorio.flujo.domain.model.ExpenseReport { *; }
-keep class dev.ycosorio.flujo.domain.model.ExpenseItem { *; }
-keep enum dev.ycosorio.flujo.domain.model.ExpenseReportStatus { *; }

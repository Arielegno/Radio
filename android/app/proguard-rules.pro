# Radio de ruta: la app no usa reflexión. Se silencian anotaciones opcionales de librerías.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn kotlin.concurrent.atomics.**

# Voces propias: el motor nativo lee estas clases por su nombre
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.brotli.**
-dontwarn org.tukaani.**
-dontwarn com.github.luben.**
-dontwarn org.objectweb.asm.**

# El motor de voz llama desde C++ a «invoke» de las funciones de Kotlin y crea GeneratedAudio por su nombre:
# si R8 les cambia el nombre, la app se cierra al hablar (pasaba al tocar «Probar»).
-keep class kotlin.jvm.functions.** { *; }
-keep class kotlin.Unit { *; }
-keepclassmembers class * implements kotlin.jvm.functions.Function1 { public java.lang.Object invoke(java.lang.Object); }

# Para que los reportes de errores digan en qué archivo y línea del código pasó el error
# (sin esto, los nombres salen cambiados por letras y no se entiende nada)
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

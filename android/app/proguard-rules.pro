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

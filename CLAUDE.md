# Notas para trabajar en Radio de ruta

- Todo en español, explicado sin tecnicismos: el dueño no programa.
- **Nunca** poner el nombre real del dueño en archivos ni en commits (firma: «Arielegno»).
- **Changelog obligatorio:** en cada versión nueva de la app, subir `versionCode`/`versionName`
  (android/app/build.gradle.kts y el texto de versión en ui/AjustesUI.kt) y agregar lo nuevo arriba en
  `android/app/src/main/java/io/github/arielegno/radio/core/Novedades.kt` **y** en `CHANGELOG.md`.
- La app se compila en GitHub Actions (rama `kotlin`) y el APK se publica en el release `android`.
- La Revista nativa (revista-kmp/) se compila para el navegador y también entra al APK.
- Los datos grandes (biografías, fichas, guiones de Luna…) van en `Almacen`, no en los ajustes.
- Si la app se cierra, el dueño puede compartir el reporte desde Ajustes › Acerca de › Reportes de errores.

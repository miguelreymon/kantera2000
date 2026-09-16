# Kantera 🏀⚡

Aplicación móvil nativa para Android desarrollada con **Kotlin** y **Jetpack Compose**, especializada en visión por computador en tiempo real e inteligencia táctica para entrenamientos de baloncesto.

Repositorio: [https://github.com/miguelreymon/kanteragameprov1.git](https://github.com/miguelreymon/kanteragameprov1.git)

---

## 🌟 Características Principales

1. **Visión por Computador e IA en Tiempo Real**:
   - **Detector de Pelota de Baloncesto (TFLite)**: Modelo `best_float32.tflite` para detección y tracking de pelota frame a frame.
   - **Estimación de Pose (MediaPipe Tasks Vision)**: Modelo `pose_landmarker_lite.task` para captura de articulaciones del jugador en tiempo real.
   - **Mapeo de Pista y Calibración**: Transformación de perspectiva (homografía) para proyectar posiciones en pista cenital 2D.

2. **Módulos de Entrenamiento y HUD**:
   - **Defend the Zone (Ghost Defender)**: Simulación de defensor virtual con zonas defensivas reactivas.
   - **60s Reaction Points**: Ejercicio dinámico de botes y reacciones con combinaciones de regate.
   - **Kids Mini Basket Drill**: Modo adaptado para jóvenes jugadores con retroalimentación auditiva y visual interactiva.
   - **Voice Coach Manager**: Instrucciones de audio en tiempo real durante los ejercicios.

3. **Análisis Táctico y Estadísticas**:
   - Análisis de vídeos subidos o grabados en directo.
   - Mapa cenital de lanzamientos y mapa de calor (*Top-Down Court Analysis*).
   - Generación de reportes tácticos detallados.

4. **Sincronización en la Nube y Perfiles**:
   - Sincronización de perfiles y progresos con Supabase (`SupabaseSyncManager`, `ProfileCloudSyncDialog`).
   - Persistencia local de estadísticas del jugador (`PlayerStatsManager`).

---

## 📋 Requisitos del Sistema

- **JDK**: Java Development Kit 21 (Temurin o OpenJDK 21 recomendado).
- **Android SDK**: Compile SDK 36, Target SDK 36, Min SDK 26 (Android 8.0 Oreo o superior).
- **Android Studio**: Android Studio Ladybug / Meerkat (2024.2+) o superior.
- **Gradle**: Versión 8.11+ / Gradle Wrapper preconfigurado con Gradle 9.x.
- **Node.js**: (Opcional) v18+ si se desea utilizar los scripts de ejecución en `package.json`.

---

## 🚀 Proceso de Configuración e Instalación Local

### 1. Clonar el repositorio
```bash
git clone https://github.com/miguelreymon/kanteragameprov1.git
cd kanteragameprov1
```

### 2. Configurar variables de entorno
Copia el archivo `.env.example` a `.env`:
```bash
cp .env.example .env
```
Si utilizas integraciones con Gemini AI o servicios externos, configura las claves correspondientes dentro de `.env`.

### 3. Instalación limpia de paquetes y scripts
El proyecto incluye un archivo `package.json` para facilitar la ejecución homogénea de tareas desde entornos de desarrollo y pipelines de Node.js:
```bash
npm install
```

### 4. Compilación del proyecto

#### Con npm:
```bash
# Compilar versión debug
npm run build

# Ejecutar pruebas unitarias y Robolectric
npm run test

# Limpiar caché de compilación
npm run clean
```

#### Con Gradle directamente:
```bash
# Compilar versión debug (APK)
gradle assembleDebug

# Ejecutar pruebas unitarias
gradle :app:testDebugUnitTest

# Limpiar build
gradle clean
```

---

## 📦 Instrucciones de Despliegue Paso a Paso

### Opción A: Despliegue en Dispositivo Físico o Emulador (Modo Depuración)

1. Conecta tu dispositivo Android mediante USB y habilita la **Depuración por USB** en las opciones de desarrollador (o inicia un Emulador con soporte de cámara).
2. Verifica la conexión con ADB:
   ```bash
   adb devices
   ```
3. Compila e instala el APK de depuración:
   ```bash
   gradle installDebug
   ```
   *Alternativamente:*
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Otorga los permisos de cámara y notificaciones cuando la aplicación lo solicite al iniciar.

---

### Opción B: Generación de APK / AAB de Producción (Release)

1. **Crear o ubicar tu Keystore de firma**:
   Si no dispones de uno, genera un keystore con `keytool`:
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Definir las variables de entorno de firma**:
   ```bash
   export KEYSTORE_PATH="/ruta/hacia/my-upload-key.jks"
   export STORE_PASSWORD="tu_password_almacen"
   export KEY_PASSWORD="tu_password_clave"
   ```
3. **Generar el Android App Bundle (AAB) para Google Play**:
   ```bash
   npm run bundle
   # o bien:
   gradle bundleRelease
   ```
   El paquete firmado se generará en:
   `app/build/outputs/bundle/release/app-release.aab`

4. **Generar el APK universal de Release**:
   ```bash
   npm run build:release
   # o bien:
   gradle assembleRelease
   ```
   El archivo generado se ubicará en:
   `app/build/outputs/apk/release/app-release.apk`

---

## 🔄 Flujo de Integración Continua Automatizada (CI)

El proyecto incluye un flujo de trabajo de **GitHub Actions** en `.github/workflows/ci.yml`.

### Comportamiento del Workflow:
- Se activa automáticamente en cada `push` o `pull_request` a las ramas `main` y `master`.
- Configura un entorno con **Ubuntu Latest** y **Java 21 (Temurin)**.
- Activa la caché inteligente de Gradle para acelerar builds subsiguientes.
- Ejecuta el conjunto de pruebas unitarias y pruebas Robolectric (`gradle :app:testDebugUnitTest`).
- Compila el APK de depuración (`gradle assembleDebug`).
- Sube el artefacto resultante `app-debug.apk` a los artefactos de la ejecución en GitHub Actions (disponible para descarga durante 7 días).

---

## 🛠️ Solución de Problemas Frecuentes (FAQ)

### 1. Error: `Unresolved reference 'TextButton'` en `KidsMiniBasketHUD.kt`
- **Causa**: Falta el import de Material 3 `androidx.compose.material3.TextButton`.
- **Solución**: Asegurarse de tener incluida la línea `import androidx.compose.material3.TextButton` en los imports de `KidsMiniBasketHUD.kt`. (Ya corregido en este repositorio).

### 2. Error al cargar modelos `.tflite` o `.task` en tiempo de ejecución
- **Causa**: El compresor de recursos de Android comprime por defecto archivos binarios grandes en la carpeta `assets/`, provocando fallos al mapear memoria con TFLite / MediaPipe.
- **Solución**: En `app/build.gradle.kts`, verificar que está declarada la directiva:
  ```kotlin
  androidResources {
      noCompress += listOf("tflite", "task")
  }
  ```

### 3. Permiso de cámara denegado o pantalla en negro
- **Causa**: Android 6.0+ requiere solicitar permisos de cámara en tiempo de ejecución. Además, en Android 13+ (API 33+) se requiere `POST_NOTIFICATIONS` para notificaciones en segundo plano.
- **Solución**: Asegurarse de aceptar el diálogo de permisos al iniciar la vista de cámara o conceder el permiso manualmente en *Ajustes > Aplicaciones > Kantera > Permisos > Cámara*.

### 4. Fallo de compatibilidad de Java (Unsupported class file major version)
- **Causa**: Se está utilizando una versión de Java inferior a JDK 21 (p. ej. JDK 11 o JDK 17) para compilar un proyecto configurado con soporte para Java 21 y AGP moderno.
- **Solución**: Configurar `JAVA_HOME` apuntando a JDK 21:
  ```bash
  export JAVA_HOME="/ruta/a/jdk-21"
  ```
  En Android Studio: *Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK* y seleccionar **JDK 21**.

### 5. `Missing google-services.json` durante el build
- **Causa**: La integración con Firebase AI / Services espera `google-services.json`.
- **Solución**: El proyecto tiene configurado `missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN` para permitir compilación local sin el archivo. Si vas a utilizar servicios de Firebase en producción, descarga tu `google-services.json` desde la consola de Firebase y colócalo en `app/google-services.json`.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo de licencia para más información.
"# kanteraaaaaaaaaaaaa" 

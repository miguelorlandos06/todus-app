# toDus — aplicación de mensajería (v0.0.1)

Implementación open-source de un cliente de mensajería para el protocolo **toDus** (cubano),
en **Kotlin + Jetpack Compose**, arquitectura MVVM + Clean.

> ⚠️ Este proyecto usa el protocolo de la aplicación oficial toDus (decompilada, ya obsoleta)
> y un flujo de login conocido como "login con número de teléfono solo" (`/v2/auth/token` con un
> secreto débil hardcodeado, descrito en `guía.txt` §2.2). El servidor oficial puede parchear ese
> flujo en cualquier momento; si lo hace, hay que migrar al flujo SMS completo (reserve → register
> → token), que también está implementado en `AuthClient`.

## Estructura

```
todus-app/
├── .github/workflows/build.yml      # CI: compila APK armeabi-v7a en GitHub Actions
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/                  # wrapper (generar con `gradle wrapper`)
├── app/
│   ├── build.gradle.kts            # version 0.0.1, ABI armeabi-v7a, Compose, Hilt, Room
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                    # strings (es), colors, themes, vector logo, mipmaps
│       └── java/cu/todus/android/
│           ├── TodusApp.kt         # @HiltAndroidApp + canal de notif.
│           ├── MainActivity.kt     # Single-activity Compose host
│           ├── protocol/           # Capa de red (no Android)
│           │   ├── TodusConstants.kt   # endpoints, versiones, límites
│           │   ├── Namespaces.kt       # jc/x1/x2/x4/x8/uc1/...
│           │   ├── auth/              # AuthClient (HTTP + protobuf manual)
│           │   └── xmpp/              # XmppSession, StanzaParser, StanzaBuilder
│           ├── data/                # Persistencia + repositorios
│           │   ├── SessionStore.kt     # DataStore (phone, token, profile)
│           │   ├── MessagingRepository.kt  # puente XMPP ⇆ Room
│           │   └── db/TodusDatabase.kt # chats, mensajes, contactos
│           ├── service/XmppService.kt   # foreground service
│           └── ui/
│               ├── theme/            # Color + Theme Material3
│               ├── components/        # Avatar, StatusDot, MessageStatusIcon, ConnectionStatusBar
│               ├── nav/RootNavGraph.kt # 10 pantallas, animaciones
│               ├── viewmodel/        # Auth, Home, Conversation, Profile
│               └── screens/          # Welcome, Phone, ProfileInitial, Home, Contacts,
│                                  # Conversation, MyProfile, EditProfile, ChangeTodusId, UserProfile
└── docs/
    └── PROTOCOL.md                 # resumen del protocolo (basado en guía.txt)
```

## Protocolo

Ver `docs/PROTOCOL.md` para el resumen completo. Resumen ejecutivo:

- **Autenticación**: HTTP POST a `https://auth.todus.cu/v2/auth/token` con body protobuf manual
  (wire-type 2 length-delimited). El login por teléfono solo usa `User-Agent: ToDus 2.1.1` y
  `Content-Type: application/octet-stream`; el campo 1 es el teléfono (10 dígitos: 53 + 8) y
  el campo 2 es el secreto `fake1234567890abcdef12345678`. La respuesta trae un JWT en el campo 1.
- **XMPP**: socket TLS a `im.todus.cu:1756`. Handshake:
  1. `<stream:stream xmlns='jc' o='im.todus.cu' xmlns:stream='x1' v='1.0'>`
  2. SASL PLAIN: `<ah xmlns='ah:ns' e='PLAIN'>base64(\0 phone \0 token)</ah>` → `<ok xmlns='x2'/>`
  3. Stream restart + `<iq i='…' t='set'><b1 xmlns='x4'><re>md5(phone)_Android</re></b1></iq>`
  4. Presence inicial: `<p xmlns='jc'><status></status></p>`
  5. GetOffline: `<iq i='…' t='get'><query xmlns='t:offline'/></iq>`
  6. Keepalive cada 30 s: `<iq i='…' t='get'><ping xmlns='urn:xmpp:ping'/></iq>`
- **Mensajes de texto**: `<m o='5350000000@im.todus.cu' t='c' i='<32hex>' xmlns='jc'>
  <k xmlns='x8'/><b>texto</b></m>`. El `<k xmlns='x8'/>` es obligatorio.
- **Recibos**: `<dd xmlns='x8' i='msg_id'/>` = entregado; `<rd xmlns='x8' i='msg_id'/>` = leído.
- **Chat states**: `<csp xmlns='uc1'/>` = composing, `<csc xmlns='uc1'/>` = paused (¡invertido
  respecto a versiones viejas del SDK!).
- **Offline**: cada mensaje offline va precedido por `<todus_offline ts='…'/>` y el bloque
  termina con `<todus_end_offline/>`; luego se confirman los IDs con
  `<iq t='set'><query xmlns='t:offline:del'><id>…</id></query></iq>`.

## Compilar

### Requisitos
- JDK 17
- Android SDK 34 (compileSdk) + Build-Tools 34.0.0
- Android NDK **no es necesario** (no hay código nativo)
- Gradle 8.7 (incluido en el wrapper)

### Generar el wrapper

Por tamaño, este repositorio no incluye `gradlew` ni `gradle/wrapper/gradle-wrapper.jar`.
Tras clonar, genera el wrapper una sola vez con cualquier Gradle ≥ 8.7:

```bash
cd todus-app
gradle wrapper --gradle-version 8.7 --distribution-type bin
chmod +x gradlew
```

> El workflow de CI (`.github/workflows/build.yml`) ya hace este paso automáticamente, así
> que no necesitas generar el wrapper localmente para que el CI compile.

### Compilar el APK (armeabi-v7a)

```bash
./gradlew :app:assembleRelease
```

El APK sale en `app/build/outputs/apk/armeabi-v7a/release/app-armeabi-v7a-release.apk`.

### CI

`.github/workflows/build.yml` compila debug + release en cada push. Los artefactos quedan
publicados en la pestaña "Actions" del repo.


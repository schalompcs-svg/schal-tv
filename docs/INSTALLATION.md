# Installation

## Sur un émulateur ou un téléphone Android (v0.2, build debug)

1. Compilez l'APK debug (voir `docs/COMPILATION.md`) :
   `./gradlew assembleDebug`
2. Récupérez `app/build/outputs/apk/debug/app-debug.apk`.
3. Sur l'appareil : autoriser "Sources inconnues" / "Installer des
   applications inconnues" pour la source utilisée (ou installer directement
   via `adb install app-debug.apk` si l'appareil est connecté en USB avec le
   débogage USB activé).

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Configurer l'adresse du serveur SCHALOM

`MainActivity` initialise `SchalomConnector(this, baseUrl = null)`. Tant que
`baseUrl` est `null`, l'app fonctionne uniquement en mode catalogue
local/hors ligne (comportement voulu tant qu'aucune adresse réelle n'est
fournie — aucune adresse n'est inventée). Pour connecter votre backend
SCHALOM réel (`server.js` / Node.js déjà en place sur votre projet SCHALOM),
remplacez cette ligne par l'URL réelle de votre API, par exemple :

```kotlin
schalomConnector = SchalomConnector(this, baseUrl = "https://votre-domaine.example")
```

Le connecteur appelle `GET {baseUrl}/api/catalog` — à adapter si votre route
réelle diffère (voir vos routes `/api/...` existantes dans SCHALOM).

## Important — ITEL IT2160

**MATÉRIEL NÉCESSAIRE / DÉPEND DU FIRMWARE.** L'APK produit par ce projet est
un APK Android : il ne s'installe **pas** sur l'ITEL IT2160, qui n'est pas un
téléphone Android. Cette version sert de plateforme de développement et de
validation de la logique métier (catalogue, favoris, navigation abstraite)
en vue d'un futur portage SCHAL BASIC — voir
`docs/LIMITES_ET_PROCHAINES_ETAPES.md`.

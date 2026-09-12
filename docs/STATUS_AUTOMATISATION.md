# SCHAL-TV — automatisation

## État

SCHAL-TV est une vraie application Android native.

Le projet utilise :

- Kotlin
- Android
- Media3 / ExoPlayer
- Gradle
- GitHub Actions
- catalogue local
- cache local
- favoris
- historique
- recherche
- filtres
- classement A-Z
- validation automatique des flux

## Build

Le téléphone n'est pas utilisé comme environnement principal de compilation.

GitHub Actions utilise :

- Ubuntu
- Java 17
- Gradle 8.7

Les APK sont publiés comme artefacts :

- SCHAL-TV-debug
- SCHAL-TV-release

## Catalogue

Le catalogue automatique utilise des données publiques.

Aucun flux n'est fabriqué.

Les chaînes sans flux sont exclues.

Les flux marqués comme géobloqués par la source sont exclus.

Les flux sont testés depuis le runner GitHub avant d'entrer dans
le catalogue validé.

La disponibilité peut dépendre de la région réseau.

## Hors ligne

L'application conserve :

- catalogue déjà téléchargé
- favoris
- historique
- préférences

Les flux Internet nécessitent évidemment une connexion.

## Priorité

Fiabilité > quantité.

Aucun nombre de chaînes n'est inventé.

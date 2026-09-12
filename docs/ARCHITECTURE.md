# Architecture — SCHAL TV v0.2

## Vue d'ensemble

```
                     ┌────────────────────┐
                     │      ui/           │  MainActivity, PlayerActivity,
                     │                    │  ChannelAdapter
                     └─────────┬──────────┘
                               │ implémente NavigationController
                               │ (UP/DOWN/LEFT/RIGHT/OK/BACK/MENU)
              ┌────────────────┼─────────────────────┐
              ▼                ▼                      ▼
        ┌───────────┐   ┌─────────────┐        ┌────────────┐
        │ catalog/  │   │  player/    │        │  offline/  │
        │ Catalog   │   │  PlayerMgr  │        │  LocalMedia│
        │ Repository│   │  (media3)   │        │  Scanner   │
        └─────┬─────┘   └─────────────┘        └────────────┘
              │
     ┌────────┼─────────┐
     ▼        ▼          ▼
┌─────────┐ ┌────────┐ ┌──────────┐
│ schalom/│ │ cache/ │ │ storage/ │
│ Connector│ │ Cache  │ │Favorites,│
│ (réseau)│ │ Manager│ │  Prefs   │
└─────────┘ └────────┘ └──────────┘
```

`core/` contient les modèles partagés (`TvChannel`, `LocalVideo`, `Favorites`,
`CacheManifest`) et `NavigationController`/`NavKey` : c'est la seule couche
que toutes les autres connaissent, aucune dépendance circulaire.

## Priorité offline-first (ordre réellement implémenté)

`MainActivity.loadCatalog()` :

1. `CatalogRepository.loadCatalog()` lit d'abord un catalogue déjà
   téléchargé (`filesDir/schalom_catalog.json`), sinon le catalogue embarqué
   dans les assets (vide par défaut, schéma respecté).
2. Si un réseau est disponible (`SchalomConnector.isNetworkAvailable()`),
   `maybeRefreshFromSchalom()` tente un rafraîchissement — en cas d'échec
   (timeout, serveur indisponible, adresse non configurée), l'app **continue
   silencieusement avec les données déjà chargées**, sans écran bloquant.
3. `CacheManager` note quelles entrées viennent d'être rafraîchies et jusqu'à
   quand elles sont considérées "fraîches" (`cache_manifest.json`), mais ne
   duplique pas les données du catalogue lui-même.
4. `LocalMediaScanner` (mode hors ligne pur) est indépendant du catalogue
   SCHALOM : il scanne `MediaStore` pour les vidéos réellement présentes sur
   l'appareil/la carte SD. Non encore branché sur un écran dédié en v0.2 (voir
   `docs/LIMITES_ET_PROCHAINES_ETAPES.md`).

Aucun écran blanc / crash / boucle infinie : chaque état (`Success`, `Empty`,
`Error` dans `CatalogResult`, `PlaybackState.Error` côté lecteur) a un
affichage explicite prévu dans `MainActivity` / `PlayerActivity`.

## Pourquoi un seul module Gradle `app/`

Voir la section correspondante du `README.md`. Le découpage en packages
Kotlin (`catalog`, `player`, `offline`, `cache`, `schalom`, `storage`, `ui`,
`core`) respecte déjà les frontières demandées ; le passage à de vrais
modules Gradle indépendants n'apporte de valeur que lorsque `core/` devra
être partagé avec une cible non-Android (SCHAL BASIC), ce qui n'est pas
encore le cas en v0.2.

## Lecteur vidéo : choix technique

`androidx.media3` (ExoPlayer) — justification détaillée en commentaire dans
`app/build.gradle.kts`. Résumé : support HLS natif, maintenu officiellement
par Google, gestion d'erreurs structurée (`PlaybackException.errorCode`)
que `PlayerManager.describeError()` traduit en messages utilisateur clairs
(INTERNET NÉCESSAIRE, Flux indisponible, Format non supporté...).

## Navigation abstraite (préparation téléphone à touches)

`NavKey` (`UP, DOWN, LEFT, RIGHT, OK, BACK, MENU`) et l'interface
`NavigationController` sont déjà utilisés par `MainActivity` et
`PlayerActivity` : le tactile (clics) et le D-pad (`onKeyDown`) appellent
tous deux `onNavKey()`. Une future UI SCHAL BASIC pour téléphone à touches
pourra implémenter un nouvel écran qui appelle exactement les mêmes méthodes
`onNavKey()`, sans dupliquer la logique de sélection/lecture/volume.

STATUT DE CETTE COMPATIBILITÉ : la logique est prête, mais aucun portage
réel vers un téléphone à touches n'existe. DÉPEND DU FIRMWARE pour l'ITEL
IT2160 — voir `docs/LIMITES_ET_PROCHAINES_ETAPES.md`.

## Format de données

Le format `schalom_catalog.json` implémenté dans `CatalogRepository` suit
exactement le document de format fourni (schema_version, categories.tv[],
champs id/type/name/.../stream_status). `favorites.json` et
`cache_manifest.json` sont bien des fichiers séparés du catalogue distant,
comme demandé.

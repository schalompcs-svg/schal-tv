# Résultats des tests

Rappel honnête : cet environnement n'a ni SDK Android ni accès réseau pour
télécharger Gradle/les dépendances. Les tests ci-dessous distinguent donc
clairement ce qui a été **réellement exécuté ici** de ce qui **doit être
vérifié sur votre machine** avant de considérer la v0.2 comme validée.

| # | Test demandé | Statut ici | Détail |
|---|---|---|---|
| 1 | Compilation | ⏳ À FAIRE sur votre machine | Pas de SDK/Gradle réseau disponible ici. Voir `docs/COMPILATION.md`. |
| 2 | Démarrage | ⏳ À FAIRE sur votre machine | Nécessite un émulateur ou appareil réel. |
| 3 | Affichage du catalogue | ✅ Logique vérifiée | `CatalogRepositoryTest` + réplique Python : le catalogue vide affiche "AUCUN CONTENU HORS LIGNE DISPONIBLE" (`CatalogResult.Empty`), un catalogue rempli filtre bien les chaînes inactives. |
| 4 | Recherche | ✅ Logique relue | `applyFilter()` filtre par sous-chaîne insensible à la casse sur `TvChannel.name`. Pas d'exécution UI réelle possible ici. |
| 5 | Favoris | ✅ Logique relue | `FavoritesStore` lit/écrit `favorites.json` séparément du catalogue ; `toggleTvFavorite` testé mentalement ligne à ligne (ajout/retrait d'un `Set`). |
| 6 | Lecture locale | ⏳ À FAIRE sur votre machine | Nécessite un vrai fichier vidéo sur un appareil réel pour valider `LocalMediaScanner` + `PlayerManager.play()` avec un chemin `file://`. |
| 7 | Absence d'Internet | ✅ Logique relue | `SchalomConnector.isNetworkAvailable()` retourne `false` sans réseau ; `maybeRefreshFromSchalom()` ne fait alors rien et `MainActivity` continue avec le catalogue déjà chargé (aucun blocage). |
| 8 | Flux indisponible | ✅ Logique relue | `PlayerManager.describeError()` mappe les codes d'erreur media3 (`ERROR_CODE_IO_*`) vers des messages explicites ("Flux indisponible", "INTERNET NÉCESSAIRE"), jamais de crash silencieux. |
| 9 | Catalogue vide | ✅ Vérifié en conditions réelles | `python3 tools/validate_catalog.py app/src/main/assets/schalom_catalog.json` → `SCHALOM CATALOG: VALID`. Le catalogue livré est réellement vide (aucune chaîne inventée). |
| 10 | Fichier vidéo incompatible | ✅ Logique relue | `PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED` / `MANIFEST_MALFORMED` → message "Format non supporté" plutôt qu'un crash. |
| 11 | Rotation si applicable | ✅ Logique relue | `PlayerActivity` déclare `android:configChanges="orientation|screenSize|keyboardHidden"` pour éviter un redémarrage brutal pendant la lecture ; `MainActivity` reste en portrait fixe (écran de liste). |
| 12 | Retour Android | ✅ Logique relue | `PlayerActivity.onNavKey(NavKey.BACK)` ferme l'écran (`finish()`) et sauvegarde la position de lecture avant (`onPause`). |
| 13 | Mémoire | ⏳ À FAIRE sur votre machine (profiler réel nécessaire) | Choix faits pour limiter l'empreinte : pas de framework lourd, `ExoPlayer.release()` appelé dans `onDestroy()`, pas de chargement de tout le catalogue distant en mémoire UI au-delà de la liste filtrée affichée. |
| 14 | Crash potentiel | ✅ Logique relue | Tous les chemins d'erreur identifiés (JSON malformé, cache corrompu, permission de stockage refusée, flux vide/invalide, réponse HTTP non-2xx, timeout) retournent un état géré plutôt que de laisser une exception remonter jusqu'au thread UI. |

## Tests unitaires JVM fournis

`app/src/test/java/com/schal/tv/CatalogRepositoryTest.kt` couvre :
- catalogue vide → aucune chaîne
- chaîne sans `id` ou sans `name` → rejetée
- chaîne `is_active=false` → filtrée
- chaîne sans flux → `stream_url` reste vide, jamais fabriqué
- chaîne avec flux → type et statut corrects, `stream_status` reste
  `UNKNOWN` tant qu'aucune vérification réseau réelle n'a eu lieu
- JSON malformé → exception `JSONException` propagée proprement (à
  capturer par l'appelant, ce que fait `CatalogRepository.loadCatalog()`
  en production)

Pour les exécuter réellement : `./gradlew testDebugUnitTest` (nécessite le
SDK/Gradle, non disponible ici).

## Ce qui manque avant une v0.2 "prête à distribuer"

Voir `docs/LIMITES_ET_PROCHAINES_ETAPES.md`.

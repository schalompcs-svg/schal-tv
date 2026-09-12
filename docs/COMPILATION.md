# Compilation

## Ce qui a été vérifié dans cet environnement de développement

Cet environnement ne dispose ni du SDK Android, ni de Gradle avec accès
réseau (pour télécharger le wrapper Gradle et les dépendances Maven). Il a
donc été **impossible d'exécuter `./gradlew build` ou de générer un APK ici**.

Ce qui a réellement été vérifié :
- `tools/validate_catalog.py` a été exécuté avec succès sur le catalogue vide
  livré (`SCHALOM CATALOG: VALID`) et sur un catalogue volontairement invalide
  (toutes les erreurs attendues ont bien été détectées).
- La logique de filtrage/parsing du catalogue (`CatalogRepository.parseChannels`)
  a été rejouée en Python à partir du même jeu de données que le test Kotlin
  `CatalogRepositoryTest`, avec un résultat conforme.
- Chaque fichier Kotlin a été relu pour cohérence des types, des imports et
  du cycle de vie Android (pas d'exécution réelle du compilateur Kotlin, qui
  n'est pas installé ici).

Ce qui n'a **pas** pu être vérifié ici et reste à faire sur votre machine :
compilation Kotlin réelle, résolution des dépendances Gradle/Maven,
génération de l'APK, exécution sur émulateur ou appareil physique.

## Prérequis sur votre machine

- Android Studio (Koala ou plus récent) **ou** un JDK 17 + Android SDK
  command-line tools installés séparément.
- SDK Platform 34, Build-Tools correspondant.
- Connexion Internet pour la première synchronisation Gradle (téléchargement
  du wrapper + des dépendances déclarées dans `app/build.gradle.kts`).

## Générer le wrapper Gradle

Ce projet ne contient pas de binaire `gradle-wrapper.jar` (fichier binaire
non généré dans cet environnement sans réseau). Sur votre machine, avec
Gradle déjà installé une fois (ou via Android Studio qui le propose
automatiquement à l'ouverture du projet) :

```bash
cd schal-tv
gradle wrapper --gradle-version 8.7
```

Ou plus simplement : ouvrez le dossier `schal-tv/` dans Android Studio — il
proposera de générer le wrapper et de synchroniser automatiquement.

## Compiler en ligne de commande (après génération du wrapper)

```bash
cd schal-tv
./gradlew assembleDebug
```

L'APK debug sera généré dans :

```
app/build/outputs/apk/debug/app-debug.apk
```

## Compiler une version release (signée)

```bash
./gradlew assembleRelease
```

Nécessite une configuration de signature (`signingConfigs`) non incluse ici
car elle dépend de votre propre keystore — voir la documentation officielle
Android sur la signature d'application.

## Exécuter les tests unitaires

```bash
./gradlew testDebugUnitTest
```

Voir `docs/TESTS.md` pour le détail des tests couverts et non couverts.

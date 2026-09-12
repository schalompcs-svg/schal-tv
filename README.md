# SCHAL TV — v0.2 (Android)

Première application de la plateforme **SCHAL iOS** (nom de plateforme logicielle
maison — sans rapport avec Apple iOS).

```
Android
  ↓
SCHAL TV        ← ce projet
  ↓
SCHALOM         ← catalogue / backend distant
  ↓
SCHAL iOS BASIC ← future couche pour téléphones à touches
  ↓
adaptation ITEL IT2160 (ou équivalent)
```

## Statut

STATUT : **HORS LIGNE** pour la lecture locale, **INTERNET NÉCESSAIRE** pour les
flux distants SCHALOM. Aucun flux n'est fabriqué : une chaîne sans `stream_url`
est affichée comme *"Flux non configuré"*, jamais comme un flux fonctionnel.

Aucun test n'a pu être exécuté sur un vrai appareil ni via `gradlew` dans cet
environnement (pas de SDK Android, pas d'accès réseau pour télécharger le
wrapper Gradle / les dépendances Maven). Le code a été relu ligne par ligne et
compile logiquement (types, imports, cycle de vie Android cohérents), mais
**la compilation réelle reste à faire sur votre machine** — voir
`docs/COMPILATION.md`.

## Pourquoi une seule Gradle module `app/` au lieu de `core/ player/ catalog/...`

La structure de dossiers demandée est respectée **en tant que packages Kotlin**
à l'intérieur du module `app` (`com.schal.tv.core`, `.catalog`, `.player`,
`.offline`, `.cache`, `.schalom`, `.ui`, `.storage`) plutôt qu'en modules
Gradle séparés. Un module par dossier ajouterait de la complexité de build
(plus de `build.gradle.kts`, plus de temps de synchronisation) sans bénéfice
réel à ce stade (v0.2, un seul développeur, pas encore de réutilisation
multi-plateforme). Le découpage en modules Gradle réels est prévu pour une
version ultérieure quand `core/` devra être partagé avec une future cible
non-Android (voir `docs/ARCHITECTURE.md`).

## Structure

```
schal-tv/
├── README.md
├── docs/
│   ├── ARCHITECTURE.md
│   ├── COMPILATION.md
│   ├── INSTALLATION.md
│   ├── TESTS.md
│   └── LIMITES_ET_PROCHAINES_ETAPES.md
├── tools/
│   └── validate_catalog.py
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/
        │   │   └── schalom_catalog.json      (catalogue vide, schéma respecté)
        │   ├── java/com/schal/tv/
        │   │   ├── core/       (modèles + navigation abstraite UP/DOWN/OK/BACK/MENU)
        │   │   ├── catalog/    (chargement + parsing du catalogue SCHALOM)
        │   │   ├── schalom/    (connecteur réseau vers l'API SCHALOM)
        │   │   ├── offline/    (scan vidéos locales + carte SD)
        │   │   ├── cache/      (cache_manifest.json)
        │   │   ├── storage/    (favoris.json, préférences, reprise de lecture)
        │   │   ├── player/     (lecteur vidéo, media3/ExoPlayer)
        │   │   └── ui/         (écran d'accueil, lecteur plein écran, adaptateurs)
        │   └── res/
        └── test/               (tests unitaires JVM : parsing du catalogue)
```

## Catalogue intégré

Le catalogue livré dans `app/src/main/assets/schalom_catalog.json` est
généré directement depuis votre vraie base `backend/data/schalom.db`
(fournie dans `SCHALOM-DEPLOIEMENT-FINAL.zip`) : 51 368 chaînes TV, 25
radios, et le reste des catégories (films, séries, dessins animés, anime,
jeux, applications, actualités, livres) telles qu'elles existent réellement
dans la base. Aucune valeur absente n'a été inventée (langue/logo/date
manquants → laissés vides). Validé : `SCHALOM CATALOG: VALID`.

⚠️ Fichier volumineux (~25 Mo, surtout à cause des 51 368 chaînes TV) : à
surveiller sur téléphones très modestes — une pagination/API distante plutôt
qu'un asset embarqué sera préférable à terme (voir
`docs/LIMITES_ET_PROCHAINES_ETAPES.md`).


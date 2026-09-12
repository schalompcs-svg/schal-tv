# Limites connues et prochaines étapes

## Limites connues (v0.2)

- **Pas d'APK fourni directement** : cet environnement n'a ni SDK Android ni
  accès réseau pour télécharger Gradle/les dépendances. Vous devez compiler
  sur votre machine (Android Studio) — voir `docs/COMPILATION.md`.
- **Catalogue vide** : aucun fichier `schalom-resultat-*.txt` réel n'a été
  fourni ; seul le document de format l'a été. Dès que vous fournissez le
  vrai export SCHALOM, il faut écrire un import (non fait en v0.2) qui
  transforme ce fichier en `schalom_catalog.json` conforme au schéma.
- **`SchalomConnector.baseUrl` non configuré** (`null`) : l'app fonctionne
  donc uniquement en mode local/hors ligne tant que vous ne renseignez pas
  l'adresse réelle de votre backend SCHALOM (`docs/INSTALLATION.md`).
- **Écran de vidéos locales non séparé** : `LocalMediaScanner` existe et
  fonctionne, mais n'est pas encore branché sur un onglet/écran dédié dans
  l'UI v0.2 (actuellement seul le catalogue TV distant/local est affiché).
- **Icône de lancement provisoire** : `ic_launcher.xml` est un simple
  rectangle vectoriel, pas un vrai logo — à remplacer par le graphisme réel
  (vous aviez mentionné vouloir créditer un graphiste sur votre projet
  Schal IA : la même logique de crédit peut s'appliquer ici).
- **Radio non implémentée** : volontairement absente (SCHAL RADIO est un
  projet séparé, comme demandé).
- **Récepteur TV externe** : non implémenté, prévu uniquement dans
  l'architecture à terme (`External Receiver API`) — HORS LIGNE et
  MATÉRIEL NÉCESSAIRE le jour où cette fonction existera réellement.
- **ITEL IT2160** : aucun portage réel. Reste DÉPEND DU FIRMWARE tant que
  chipset/SDK/format d'application ne sont pas identifiés.
- **Tests instrumentés Android (UI réelle)** : non exécutables ici, à faire
  sur émulateur/appareil (voir `docs/TESTS.md`).

## Prochaines étapes recommandées (dans l'ordre)

1. Compiler le projet sur votre machine et confirmer que
   `./gradlew assembleDebug` réussit (voir `docs/COMPILATION.md`).
2. Installer l'APK debug sur un appareil réel et vérifier les 14 scénarios de
   `docs/TESTS.md` en conditions réelles.
3. Fournir un vrai fichier `schalom-resultat-*.txt` pour écrire l'import qui
   remplit `schalom_catalog.json` avec vos ~52 000 chaînes.
4. Configurer `SchalomConnector.baseUrl` avec l'adresse réelle de votre
   backend SCHALOM.
5. Ajouter un écran/onglet dédié aux vidéos locales (`LocalMediaScanner` est
   déjà prêt côté logique).
6. Remplacer l'icône de lancement provisoire par un vrai logo SCHAL TV.
7. Une fois la v0.2 stable, envisager la séparation en modules Gradle réels
   si `core/` doit être partagé avec une future cible non-Android.

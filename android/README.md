# Radios — application Android

Kotlin · Jetpack Compose (Material 3) · Media3 ExoPlayer + MediaSessionService · Coil · DataStore.

## Ouvrir dans Android Studio

1. **File ▸ Open…** → sélectionner la **racine du dépôt**
   (c'est elle qui contient `settings.gradle.kts` ; le module `app` est dans `android/app/`).
2. Laisser la synchro Gradle se terminer. Gradle JDK : le **JBR embarqué**
   d'Android Studio (*Settings ▸ Build Tools ▸ Gradle ▸ Gradle JDK*).
   Ne pas utiliser le JDK 28-ea du `PATH`.
3. Choisir l'émulateur `Medium Phone API 37` et une run configuration :

| Run configuration (`.run/` à la racine) | Ce qu'elle fait |
|---|---|
| **Radios** | installe et lance l'app |
| **Tests unitaires (config)** | valide `config/radios.json` sur la JVM, sans émulateur |
| **Tests instrumentés (émulateur)** | tests sur l'appareil : assets, logos, TLS + accès réseau à tous les flux |
| **Tests instrumentés (Gradle)** | mêmes tests via `connectedDebugAndroidTest` (rapport HTML) |

## Avec le Makefile (depuis la racine du dépôt)

```bash
make            # liste des cibles
make build      # APK debug
make apk        # copie l'APK dans dist/radios-<version>-debug.apk (à envoyer par mail)
make upload     # make apk + envoi sur Google Drive (gdrive-copy.sh / rclone, dossier Transferts/)
make run        # installe et lance sur l'émulateur/appareil connecté
make test       # tests unitaires ; make test-device pour les tests instrumentés
make check-streams  # vérifie depuis le PC que tous les flux de la config répondent
make emulator   # démarre l'AVD Medium_Phone
```

## En ligne de commande (depuis la racine du dépôt)

```bash
export JAVA_HOME=~/Community/android-studio/jbr
./gradlew assembleDebug                 # android/app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest             # tests JVM
./gradlew connectedDebugAndroidTest     # tests instrumentés (émulateur démarré)
./gradlew installDebug                  # installe sur l'appareil connecté
```

## Structure

```
app/src/main/java/fr/lpoirier/radios/
├── RadiosApp.kt                conteneur de dépendances
├── MainActivity.kt             point d'entrée Compose, permission notifications
├── data/                       Station (modèle), StationRepository (assets), PreferencesRepository (favoris)
├── player/                     RadioPlaybackService (Media3), RadioPlayer (titre ICY), PlayerConnection (MediaController)
└── ui/                         RadioListScreen, PlayerScreen, RadiosViewModel, thème
```

- `config/radios.json` est copié automatiquement dans les assets au build
  (tâche `copyRadioConfig`) : modifier la config à la racine suffit.
- `res/xml/network_security_config.xml` autorise le HTTP clair (3 radios) et
  embarque le certificat intermédiaire Sectigo DV R36 (`res/raw/`) pour
  `*.elitecomunicacion.es` (onda-sevilla, onda-mencia).

## Scénarios de test manuel

- Lancer une radio, verrouiller l'écran → la notification média reste, play/pause fonctionne.
- Débrancher le casque → pause automatique.
- Couper le Wi-Fi quelques secondes → reconnexion automatique (jusqu'à 5 tentatives).
- Radios avec titre en direct (ICY) : le morceau s'affiche dans le mini-lecteur et l'écran lecteur.
- Favoris : cœur sur une radio → section « ★ Favoris » en tête, conservée au redémarrage.
- Recherche : « radiole » trouve « Radiolé » (insensible aux accents).
- Genres : puces *Tous / Flamenco / Jazz / Techno / Culture / Cultura* sous le titre (sur plusieurs lignes si besoin) ; la couleur du genre s'applique
  à la liste et à l'écran lecteur, le choix est conservé au redémarrage. Une recherche
  porte toujours sur tous les genres.
- Flux HLS (RNE Radio 3, *Cultura*) : lecture via `media3-exoplayer-hls`.
- Vue *Tous* : favoris puis un carrousel horizontal par genre ;
  toucher un logo lance/met en pause la radio, « Voir tout → » ouvre la liste du genre.

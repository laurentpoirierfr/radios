# Radios — raccourcis de build.
# `make` ou `make help` affiche la liste des cibles.

# JDK utilisé par Gradle : le JBR d'Android Studio (le JDK 28-ea du PATH n'est pas supporté).
# Surchargeable : make build JAVA_HOME=/chemin/vers/jdk
JAVA_HOME   ?= $(HOME)/Community/android-studio/jbr
ANDROID_SDK ?= $(HOME)/Android/Sdk
AVD         ?= Medium_Phone

ADB      := $(ANDROID_SDK)/platform-tools/adb
EMULATOR := $(ANDROID_SDK)/emulator/emulator
GRADLE   := JAVA_HOME=$(JAVA_HOME) ./gradlew --console=plain
APP_ID   := fr.lpoirier.radios
APK_DIR  := android/app/build/outputs/apk
# Version lue dans android/app/build.gradle.kts (versionName = "x.y.z")
VERSION  := $(shell sed -n 's/.*versionName = "\(.*\)".*/\1/p' android/app/build.gradle.kts)
DIST_APK := dist/radios-$(VERSION)-debug.apk
# Script d'envoi vers Google Drive (~/Community/bin/gdrive-copy.sh, basé sur rclone).
# Surchargeable : make upload GDRIVE_COPY=/chemin/script GDRIVE_DIR=AutreDossier
GDRIVE_COPY ?= gdrive-copy.sh

export JAVA_HOME

.DEFAULT_GOAL := help
.PHONY: help build apk upload release install run test test-device check-config check-streams emulator logcat stop clean

help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

build: ## Compile l'APK debug
	$(GRADLE) assembleDebug
	@echo "→ $(APK_DIR)/debug/app-debug.apk"

apk: build ## Copie l'APK debug dans dist/ (prêt à envoyer par mail ou cloud)
	@mkdir -p dist
	cp $(APK_DIR)/debug/app-debug.apk $(DIST_APK)
	@echo "→ $(DIST_APK) ($$(du -h $(DIST_APK) | cut -f1))"

upload: apk ## Génère l'APK puis l'envoie sur Google Drive (dossier Transferts/ par défaut)
	@command -v $(GDRIVE_COPY) >/dev/null 2>&1 || { \
		echo "$(GDRIVE_COPY) introuvable : vérifie ~/Community/bin dans le PATH (ou GDRIVE_COPY=...)"; exit 1; }
	$(GDRIVE_COPY) $(DIST_APK)

release: ## Compile l'APK release (minifié, non signé)
	$(GRADLE) assembleRelease
	@echo "→ $(APK_DIR)/release/"

install: ## Compile et installe l'APK debug sur l'appareil/émulateur connecté
	$(GRADLE) installDebug

run: install ## Installe puis lance l'app
	$(ADB) shell am force-stop $(APP_ID)
	$(ADB) shell am start -n $(APP_ID)/.MainActivity

test: ## Tests unitaires JVM (validation de config/radios.json)
	$(GRADLE) testDebugUnitTest

test-device: ## Tests instrumentés sur l'appareil/émulateur (assets, TLS, accès aux flux)
	$(GRADLE) connectedDebugAndroidTest
	@echo "→ rapport : android/app/build/reports/androidTests/connected/"

check-config: ## Valide config/radios.json contre le schéma JSON (python3 + jsonschema)
	cd config && python3 -c "import json, jsonschema; \
		cfg = json.load(open('radios.json')); \
		jsonschema.validate(cfg, json.load(open('radios.schema.json'))); \
		print('OK,', len(cfg['stations']), 'stations')"

check-streams: ## Vérifie depuis le PC que tous les flux de la config répondent (sans émulateur)
	tools/probe-streams.sh --config config/radios.json

emulator: ## Démarre l'émulateur (AVD=Medium_Phone par défaut)
	$(EMULATOR) -avd $(AVD) > /dev/null 2>&1 &

logcat: ## Affiche les logs de l'app (Ctrl+C pour quitter)
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(APP_ID))

stop: ## Arrête l'app sur l'appareil
	$(ADB) shell am force-stop $(APP_ID)

clean: ## Supprime les fichiers de build
	$(GRADLE) clean

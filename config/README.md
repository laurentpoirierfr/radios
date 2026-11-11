# `config/` — Configuration des webradios par genre

Ce répertoire contient **la source de vérité** de l'application : les webradios
classées par **genre** (flamenco, jazz…) puis par **catégorie**, avec leurs flux
et leurs logos. Il est indépendant du framework
mobile choisi — n'importe quelle application (Android, iOS, desktop, web) peut
consommer `radios.json`.

## Fichiers

| Fichier | Rôle |
|---|---|
| `radios.json` | Genres et catégories, puis la liste des radios : flux, métadonnées et **logo embarqué en base64** (data URI, 128×128 px) |
| `radios.schema.json` | Schéma JSON (draft 2020-12) pour valider `radios.json` |

## Structure (version 2.x)

```jsonc
{
  "version": "2.0.0",
  "generated": "2026-11-11",
  "genres": {                                // l'ordre des clés = ordre d'affichage
    "flamenco": {
      "name": "Flamenco",
      "description": "…",
      "color": "#B3122E",                    // couleur d'accent du genre
      "categories": {                        // id (unique tous genres confondus) → libellé
        "flamenco-pur": "Flamenco pur — cante, toque, palmas",
        "sevillanas-rumba": "…",
        "flamenco-local": "…"
      }
    },
    "jazz": { … }
  },
  "stations": [ … ]
}
```

## Format d'une station

```jsonc
{
  "id": "canal-flamenco",                    // slug stable, clé pour favoris etc.
  "name": "Flamenco Radio — Canal Sur (RTVA)",
  "genre": "flamenco",                       // clé de `genres`
  "category": "flamenco-pur",                // clé de `genres.<genre>.categories`
  "description": "Webradio flamenco officielle de la RTVA…",
  "stream": "https://…/flamenco.mp3",        // URL directe du flux audio
  "homepage": "https://www.canalsur.es/…",
  "country": "ES",
  "region": "Andalousie — Séville",
  "codec": "MP3",                            // MP3 ou AAC
  "bitrateKbps": 320,
  "tags": ["flamenco", "cante", "guitare"],  // étiquettes libres, utilisées par la recherche
  "language": "es",
  "logo": "data:image/png;base64,iVBORw0KG…", // 128×128, PNG ou WebP
  "logoSourceUrl": "https://…",              // logo d'origine (attribution)
  "notes": "…"                               // optionnel : remarques techniques
}
```

## Genres et catégories

| Genre | Catégorie | Contenu | Radios |
|---|---|---|---|
| `flamenco` | `flamenco-pur` | Flamenco pur — cante, toque, palmas | 6 |
| | `sevillanas-rumba` | Sevillanas, rumba et flamenco-pop | 5 |
| | `flamenco-local` | Radios locales au programme flamenco | 3 |
| `jazz` | `jazz-reference` | Grandes radios jazz — standards, swing, be-bop, actuel | 7 |
| | `jazz-thematique` | Manouche, latin & piano — webradios thématiques | 4 |
| | `jazz-lounge` | Smooth jazz, nu jazz & lounge | 3 |
| `techno` | `techno-reference` | Grandes radios techno — Detroit, Berlin, clubs | 6 |
| | `techno-minimal-dub` | Minimal, dub & deep — styles atmosphériques | 5 |
| | `techno-hard` | Hard, acid & schranz — styles rapides et sombres | 4 |

La cohérence genre ↔ catégorie (une station ne peut pas utiliser la catégorie
d'un autre genre) n'est pas exprimable en JSON Schema : elle est vérifiée par
les tests unitaires (`make test`).

### Ajouter un genre

1. Ajouter une entrée dans `genres` (nom, description, couleur, catégories).
2. Ajouter les stations avec `"genre": "<id>"` et une catégorie de ce genre.
3. `make check-config && make test`, puis `make test-device` pour vérifier les flux depuis Android.

### Historique

- **2.1.0** — ajout du genre techno (15 radios). Logos génériques pour Minimal Mix
  et Radio Caprice (aucun logo officiel récupérable, signalé dans `notes`).
- **2.0.0** — introduction des genres : `categories` racine → `genres.<id>.categories`,
  station `genres[]` → `tags[]`, ajout de `genre` ; catégories renommées
  `flamenco` → `flamenco-pur`, `radios-locales` → `flamenco-local` ; ajout de 14 radios jazz.
- **1.0.0** — 14 radios flamenco.

## Sources des données

- **API Radio Browser** (`api.radio-browser.info`) — recensement communautaire des
  webradios, pour la découverte initiale des flux.
- **Sites officiels des radios** — vérification manuelle de chaque flux (testé le
  11/11/2026, en-têtes ICY + réception audio confirmées) et des logos.
- **radio.net** — logos Canal Flamenco et Canal Fiesta (absents du site RTVA),
  FIP et France Musique.
- **myTuner** — logo FG Techno.

## Valider la configuration

```bash
python3 -c "
import json, jsonschema
cfg = json.load(open('radios.json'))
schema = json.load(open('radios.schema.json'))
jsonschema.validate(cfg, schema)
print('OK,', len(cfg['stations']), 'stations')"
```

## Points d'attention

- **TLS incomplet** : `onda-sevilla` et `onda-mencia` (serveurs
  `elitecomunicacion.es`) n'envoient pas le certificat intermédiaire Sectigo.
  Sur Android, la connexion sera rejetée par défaut → embarquer l'intermédiaire
  dans l'app ou prévoir un trust store spécifique (champ `notes`).
- **Flux Shoutcast** : `flamenco-fm` se termine par `;` (convention Shoutcast) ;
  le serveur répond néanmoins en HTTP/1.1 — compatible ExoPlayer.
- **Métadonnées ICY** : la plupart des flux diffusent `icy-name` / `icy-br`, et
  pour certains le titre du morceau en cours (affichable dans l'app).
- Les logos appartiennent à leurs propriétaires respectifs ; le champ
  `logoSourceUrl` permet de re-télécharger l'original si besoin.

## Re-générer / mettre à jour

Le fichier a été généré par un script de collecte (téléchargement des logos,
rasterisation des SVG via `resvg`, rognage + cadrage 128×128 via Pillow,
encodage base64). Pour ajouter une radio :

1. Ajouter un bloc `{...}` dans la liste des stations (respecter le schéma).
2. Générer le logo : image carrée ou rognée, 128×128, encodée en
   `data:image/png;base64,…` ou `data:image/webp;base64,…`.
3. Valider avec la commande ci-dessus.

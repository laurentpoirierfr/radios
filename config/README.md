# `config/` — Configuration des radios flamenco

Ce répertoire contient **la source de vérité** de l'application : la liste des
webradios flamenco, leurs flux et leurs logos. Il est indépendant du framework
mobile choisi — n'importe quelle application (Android, iOS, desktop, web) peut
consommer `radios.json`.

## Fichiers

| Fichier | Rôle |
|---|---|
| `radios.json` | Liste des radios : flux, métadonnées, catégorie et **logo embarqué en base64** (data URI, 128×128 px) |
| `radios.schema.json` | Schéma JSON (draft 2020-12) pour valider `radios.json` |

## Format d'une station

```jsonc
{
  "id": "canal-flamenco",                    // slug stable, clé pour favoris etc.
  "name": "Flamenco Radio — Canal Sur (RTVA)",
  "category": "flamenco",                   // flamenco | sevillanas-rumba | radios-locales
  "description": "Webradio flamenco officielle de la RTVA…",
  "stream": "https://…/flamenco.mp3",        // URL directe du flux audio
  "homepage": "https://www.canalsur.es/…",
  "country": "ES",
  "region": "Andalousie — Séville",
  "codec": "MP3",                            // MP3 ou AAC
  "bitrateKbps": 320,
  "genres": ["flamenco", "cante", "guitare"],
  "language": "es",
  "logo": "data:image/png;base64,iVBORw0KG…", // 128×128, PNG ou WebP
  "logoSourceUrl": "https://…",              // logo d'origine (attribution)
  "notes": "…"                               // optionnel : remarques techniques
}
```

## Catégories

| Catégorie | Contenu |
|---|---|
| `flamenco` | Flamenco pur — cante, toque, palmas |
| `sevillanas-rumba` | Sevillanas, rumba et flamenco-pop |
| `radios-locales` | Radios locales au programme flamenco |

## Sources des données

- **API Radio Browser** (`api.radio-browser.info`) — recensement communautaire des
  webradios, pour la découverte initiale des flux.
- **Sites officiels des radios** — vérification manuelle de chaque flux (testé le
  11/11/2026, en-têtes ICY + réception audio confirmées) et des logos.
- **radio.net** — logos Canal Flamenco et Canal Fiesta (absents du site RTVA).

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

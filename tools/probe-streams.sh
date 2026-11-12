#!/usr/bin/env bash
# probe-streams.sh — vérifie que des flux radio répondent et envoient de l'audio.
#
#   tools/probe-streams.sh URL...            # teste les URL données
#   tools/probe-streams.sh --config [FICHIER] # teste tous les flux de config/radios.json
#
# Pour chaque flux : code HTTP final, Content-Type, débit (icy-br), nom (icy-name),
# octets reçus en quelques secondes. Code de sortie 1 si au moins un flux échoue.

set -uo pipefail

TIMEOUT="${PROBE_TIMEOUT:-6}"   # secondes d'écoute par flux
failures=0

# Autorités de certification : celles du système + les certificats embarqués par l'app
# (res/raw/*.pem, ex. l'intermédiaire Sectigo d'onda-sevilla / onda-mencia), pour que
# le test se comporte comme Android avec network_security_config.xml.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CA_BUNDLE=$(mktemp)
trap 'rm -f "$CA_BUNDLE"' EXIT
cat /etc/ssl/certs/ca-certificates.crt "$ROOT"/android/app/src/main/res/raw/*.pem > "$CA_BUNDLE" 2>/dev/null

probe() {
    local label="$1" url="$2" h b code ct name br size
    h=$(mktemp); b=$(mktemp)
    curl -sL -m "$TIMEOUT" --cacert "$CA_BUNDLE" -A "Radios-Probe/1.0" -H "Icy-MetaData: 1" -D "$h" -o "$b" "$url" 2>/dev/null
    code=$(grep -iE '^(HTTP/|ICY )' "$h" | tail -1 | awk '{print $2}')
    ct=$(grep -i '^content-type' "$h" | tail -1 | cut -d' ' -f2- | tr -d '\r' | sed 's/^[Cc]ontent-[Tt]ype: *//')
    name=$(grep -i '^icy-name' "$h" | tail -1 | cut -d' ' -f2- | tr -d '\r')
    br=$(grep -i '^icy-br' "$h" | tail -1 | cut -d' ' -f2- | tr -d '\r' | sed 's/^icy-br://')
    size=$(stat -c%s "$b")
    # HLS : la playlist est du texte ; on télécharge un segment pour vérifier qu'il y a de l'audio.
    if [[ "${ct,,}" == *mpegurl* ]] && head -c 7 "$b" | grep -q '#EXTM3U'; then   # insensible à la casse
        local seg
        seg=$(grep -v '^#' "$b" | grep -v '^[[:space:]]*$' | tail -1)
        [[ "$seg" != http* ]] && seg="${url%/*}/$seg"
        if [[ "$seg" == *.m3u8* ]]; then   # playlist maître -> playlist média
            curl -sL -m "$TIMEOUT" --cacert "$CA_BUNDLE" -o "$b" "$seg"
            local media="$seg"
            seg=$(grep -v '^#' "$b" | grep -v '^[[:space:]]*$' | tail -1)
            [[ "$seg" != http* ]] && seg="${media%/*}/$seg"
        fi
        size=$(curl -sL -m "$TIMEOUT" --cacert "$CA_BUNDLE" "$seg" | wc -c)
        ct="hls ($ct)"; br="${br:-hls}"
    fi
    rm -f "$h" "$b"
    local status="OK"
    if [[ "${code:-}" != 2* || "$size" -lt 8192 || "$ct" != audio/* && "$ct" != application/ogg* && "$ct" != hls* ]]; then
        status="ÉCHEC"; failures=$((failures + 1))
    fi
    printf "%-5s %-4s %8s o  %-18s br=%-4s %-26s %s\n" \
        "$status" "${code:-ERR}" "$size" "${ct:0:18}" "${br:0:4}" "${name:0:26}" "$label"
}

if [[ "${1:-}" == "--config" ]]; then
    config="${2:-$ROOT/config/radios.json}"
    while IFS=$'\t' read -r id url; do
        probe "$id  $url" "$url"
    done < <(python3 -c 'import json,sys
for s in json.load(open(sys.argv[1]))["stations"]: print(s["id"] + "\t" + s["stream"])' "$config")
elif [[ $# -gt 0 ]]; then
    for url in "$@"; do probe "$url" "$url"; done
else
    sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
fi

echo "---"
[[ $failures -eq 0 ]] && echo "Tous les flux répondent." || echo "$failures flux en échec."
[[ $failures -eq 0 ]]

#!/usr/bin/env python3
"""
mp3-bitrate.py — mesure le débit réel d'un flux MP3 en lisant les en-têtes de trames
(utile quand le serveur n'annonce pas icy-br, ou l'annonce mal).

    tools/mp3-bitrate.py URL...
"""
import sys, urllib.request
BR = [0,32,40,48,56,64,80,96,112,128,160,192,224,256,320]
for url in sys.argv[1:]:
    req = urllib.request.Request(url, headers={"User-Agent": "Radios-Probe/0.1"})
    data = urllib.request.urlopen(req, timeout=10).read(65536)
    rates = []; i = 0
    while i < len(data) - 4 and len(rates) < 30:
        if data[i] == 0xFF and (data[i+1] & 0xE0) == 0xE0 and ((data[i+1] >> 3) & 3) == 3 and ((data[i+1] >> 1) & 3) == 1 and 0 < (data[i+2] >> 4) < 15:
            rates.append(BR[data[i+2] >> 4]); i += 4; continue
        i += 1
    print(max(set(rates), key=rates.count) if rates else "?", "kbps", url)

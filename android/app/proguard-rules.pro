# kotlinx.serialization : les règles nécessaires sont fournies par la bibliothèque.
# Conserver les classes de configuration par sécurité (lecture de radios.json).
-keep class com.flamenco.radios.data.RadioConfig { *; }
-keep class com.flamenco.radios.data.Station { *; }

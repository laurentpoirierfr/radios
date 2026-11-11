# kotlinx.serialization : les règles nécessaires sont fournies par la bibliothèque.
# Conserver les classes de configuration par sécurité (lecture de radios.json).
-keep class fr.lpoirier.radios.data.RadioConfig { *; }
-keep class fr.lpoirier.radios.data.Station { *; }

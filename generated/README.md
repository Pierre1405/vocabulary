# Génération des ressources pour l'application

Ce répertoire contient les outils et scripts pour générer les ressources nécessaires à l'application d'apprentissage de phrases en allemand. Les ressources incluent des fichiers SQLite et des données pour l'application.

## Structure du répertoire

```
generated/
├── step 0 source/                  # Fichiers sources (TSV, etc.)
├── step 1 chunk/                  # Scripts pour diviser les fichiers en chunks
├── step 2 process/                # Scripts pour traiter les chunks
├── step 3 generate_sqlite/        # Scripts pour générer la base de données SQLite
└── README.md                      # Ce fichier
```

## Étapes de génération

### Étape 0 : Fichiers sources
- **Répertoire** : `step 0 source/`
- **Contenu** :
  - Fichiers TSV contenant les paires de phrases en allemand et français.
  - Exemple : `Sentence pairs in German-French - 2026-03-13.tsv`

### Étape 1 : Division en chunks
- **Répertoire** : `step 1 chunk/`
- **Script** : `split_tsv.py`
- **Description** :
  - Divise un fichier TSV en plusieurs chunks pour faciliter le traitement.
  - Utilise des valeurs par défaut pour les chemins et la taille des chunks.
- **Utilisation** :
  ```bash
  python split_tsv.py --input_file "chemin/vers/fichier.tsv" --output_dir "chemin/vers/sortie" --chunk_size 1000
  ```
  Ou simplement :
  ```bash
  python split_tsv.py
  ```
  (utilise les valeurs par défaut)

### Étape 2 : Traitement des chunks
- **Répertoire** : `step 2 process/`
- **Description** :
  - Scripts pour nettoyer, valider ou transformer les chunks.
  - Peut inclure des scripts pour ajouter des métadonnées ou normaliser les données.
- **Exemple de scripts** :
  - `clean_data.py` : Nettoie les données des chunks.
  - `validate_data.py` : Valide la structure des données.

### Étape 3 : Génération de la base de données SQLite
- **Répertoire** : `step 3 generate_sqlite/`
- **Description** :
  - Scripts pour générer une base de données SQLite à partir des chunks traités.
  - Crée les tables nécessaires pour l'application.
- **Exemple de scripts** :
  - `create_db.py` : Crée la base de données et les tables.
  - `import_data.py` : Importe les données des chunks dans la base de données.

## Exemple de workflow complet

1. **Préparer les fichiers sources** :
   - Placer les fichiers TSV dans `step 0 source/`.

2. **Diviser en chunks** :
   ```bash
   cd generated\step 1 chunk
   python split_tsv.py
   ```

3. **Traiter les chunks** :
   ```bash
   cd ..\step 2 process
   python clean_data.py
   python validate_data.py
   ```

4. **Générer la base de données SQLite** :
   ```bash
   cd ..\step 3 generate_sqlite
   python create_db.py
   python import_data.py
   ```

5. **Intégrer la base de données dans l'application** :
   - Copier le fichier SQLite généré dans le répertoire `app/src/main/assets/` de l'application Android.

## Notes
- Assurez-vous que les chemins dans les scripts sont corrects pour votre environnement.
- Les valeurs par défaut dans les scripts peuvent être modifiées selon vos besoins.
- Vérifiez les données à chaque étape pour garantir leur intégrité.

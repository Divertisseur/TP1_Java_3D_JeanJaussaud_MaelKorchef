# TP1_Java_3D_JeanJaussaud_MaelKorchef

## Compte rendu de TP - DataFlight Project "Catch me if you can!"

### Présentation du projet

Ce projet consiste en la création d'un utilitaire permettant d'afficher des données relatives à l'aviation de tourisme. L'application utilise une source de données statiques (liste des aéroports mondiaux au format CSV) et une source de données dynamiques (API aviationstack.com) pour afficher des informations sur les vols en temps réel.

L'interface graphique est développée avec JavaFX et propose une visualisation 3D de la Terre sur laquelle on peut interagir pour trouver des aéroports et visualiser les vols.

---

## Réalisation des questions du TP

### 1. Première session : Classes de bases et lecture CSV

#### 1.1 Classe `Aeroport`

**Fichier :** `src/Aeroport.java`

**Réalisation :**
- Création de la classe `Aeroport` avec les attributs suivants :
  - `nom` : Le nom de l'aéroport
  - `latitude` : La latitude GPS
  - `longitude` : La longitude GPS
  - `codeIATA` : Le code IATA de l'aéroport

- Implémentation des getters pour tous les attributs
- Surcharge de la méthode `toString()` pour faciliter les tests et le débogage
- Implémentation de la méthode `calculDistance(Aeroport a)` qui calcule la distance entre deux aéroports en utilisant la formule simplifiée :
  ```
  norme = (lat2-lat1)² + (lon2-lon1)² * cos²((lat2+lat1)/2)
  ```

**Tests :** La classe a été testée avec des instances simples pour vérifier le bon fonctionnement des constructeurs et des méthodes.

#### 1.2 Classe `World`

**Fichier :** `src/World.java`

**Réalisation :**
- Création de la classe `World` qui contient une liste de tous les aéroports du monde
- Implémentation du constructeur qui lit un fichier CSV :
  - Utilisation de `BufferedReader` pour lire le fichier ligne par ligne
  - Filtrage pour ne garder que les aéroports de type "large_airport"
  - Parsing des champs CSV avec gestion des guillemets
  - Extraction du code IATA (champ index 9)
  - Extraction des coordonnées GPS en utilisant une expression régulière pour trouver le pattern "longitude, latitude"
  - Création d'objets `Aeroport` et ajout à la liste

- Implémentation de la méthode `findNearestAirport(double longitude, double latitude)` :
  - Parcourt tous les aéroports de la liste
  - Calcule la distance entre le point donné et chaque aéroport
  - Retourne l'aéroport le plus proche

- Implémentation de la méthode `findByCode(String code)` :
  - Recherche un aéroport par son code IATA exact
  - Retourne l'aéroport correspondant ou `null` si non trouvé

- Implémentation de la méthode `distance(double lon1, double lat1, double lon2, double lat2)` :
  - Calcule la distance entre deux points GPS en utilisant la même formule simplifiée que dans `Aeroport`

**Tests :** La classe a été testée avec le code fourni dans le sujet :
- Chargement du fichier CSV
- Recherche de l'aéroport le plus proche de Paris (latitude : 48.866, longitude : 2.316) - devrait trouver Orly (ORY)
- Recherche de l'aéroport CDG par son code
- Calcul des distances

---

### 2. Travail à la maison : Configuration JavaFX

**Réalisation :**
- Configuration du projet pour utiliser JavaFX
- Création de la structure de base de l'application JavaFX

---

### 3. Deuxième partie : Interface Homme-Machine (IHM)

#### 3.1 Classe `Earth`

**Fichier :** `src/Earth.java`

**Réalisation :**
- Création de la classe `Earth` qui hérite de `Group` (JavaFX)
- Implémentation du constructeur :
  - Création d'une sphère de rayon 300 pixels représentant la Terre
  - Configuration d'une rotation automatique autour de l'axe Y
  - Utilisation d'un `AnimationTimer` pour animer la rotation (un tour complet en 15 secondes)
  - Ajout de la sphère au groupe

- Implémentation de la méthode `setTexture(String texturePath)` :
  - Charge une texture d'image pour la sphère
  - Utilise `PhongMaterial` avec `setDiffuseMap()` pour mapper l'image
  - Gestion des erreurs avec un matériau par défaut bleu si la texture ne peut pas être chargée

- Implémentation de la méthode `createSphere(Aeroport a, Color color)` :
  - Crée une petite sphère de rayon 2 pixels pour représenter un aéroport
  - Convertit les coordonnées GPS en coordonnées 3D en utilisant les formules :
    ```
    X = R * cos(lat) * sin(lon)
    Y = -R * sin(lat) * 1.3  (facteur de correction empirique)
    Z = -R * cos(lat) * cos(lon)
    ```
  - Applique la couleur spécifiée au matériau

- Implémentation de la méthode `displayRedSphere(Aeroport a)` :
  - Affiche une sphère rouge sur l'aéroport spécifié
  - Utilise `createSphere()` avec `Color.RED`

- Implémentation de la méthode `displayYellowSphere(Aeroport a)` :
  - Affiche une sphère jaune sur l'aéroport spécifié
  - Utilise `createSphere()` avec `Color.YELLOW`

#### 3.2 Classe `Interface`

**Fichier :** `src/Interface.java`

**Réalisation :**
- Création de la classe `Interface` qui hérite de `Application` (JavaFX)
- Implémentation de la méthode `start(Stage primaryStage)` :
  - Chargement de la liste des aéroports via la classe `World`
  - Création de l'objet `Earth`
  - Tentative de chargement de la texture de la Terre
  - Configuration de la scène avec une taille de 1200x800 pixels

- Configuration de la caméra `PerspectiveCamera` :
  - Position initiale à Z = -1000
  - `setNearClip(0.1)` et `setFarClip(2000.0)` pour définir les distances d'affichage
  - `setFieldOfView(35)` pour l'angle de vision

- Implémentation des EventHandlers pour l'interactivité :
  - **Clic gauche et drag** : Gestion du zoom en déplaçant la caméra sur l'axe Z
    - Capture de la position de la souris lors du clic
    - Calcul du déplacement lors du drag
    - Limitation du zoom entre -300 et -2000

  - **Clic droit** : Recherche de l'aéroport le plus proche
    - Utilisation de `PickResult` pour obtenir le point d'intersection en 3D
    - Conversion des coordonnées 3D en latitude/longitude :
      ```
      latitude = -arcsin(y / R)
      longitude = atan2(x, -z)
      ```
    - Recherche de l'aéroport le plus proche via `World.findNearestAirport()`
    - Affichage d'une sphère rouge sur l'aéroport trouvé
    - Appel de l'API pour récupérer les vols en direction de cet aéroport

- Implémentation de la méthode `fetchFlightsForAirport(Aeroport airport)` :
  - Utilise `CompletableFuture.runAsync()` pour exécuter la requête HTTP dans un thread séparé (évite de bloquer l'interface)
  - Construction de l'URL de l'API aviationstack.com avec la clé d'accès et le code IATA de destination
  - Utilisation de `HttpClient` (Java 11+) pour effectuer la requête HTTP
  - Parsing de la réponse JSON via `JsonFlightFiller`
  - Affichage des aéroports de départ en jaune sur le globe
  - Utilisation de `Platform.runLater()` pour mettre à jour l'interface depuis le thread de l'API

---

### 4. Troisième partie : Accès aux données "live"

#### 4.1 Classe `Flight`

**Fichier :** `src/Flight.java`

**Réalisation :**
- Création de la classe `Flight` pour modéliser un vol
- Attributs :
  - `depart` : L'aéroport de départ
  - `arrivee` : L'aéroport d'arrivée
- Implémentation des getters
- Surcharge de `toString()` pour l'affichage

#### 4.2 Classe `JsonFlightFiller`

**Fichier :** `src/JsonFlightFiller.java`

**Réalisation :**
- Création de la classe `JsonFlightFiller` pour parser les données JSON de l'API
- Utilisation du paquet JSON d'Oracle (`javax.json`)
- Implémentation du constructeur `JsonFlightFiller(String jsonString, World w)` :
  - Création d'un `JsonReader` à partir de la chaîne JSON
  - Extraction du tableau "data" contenant les vols
  - Pour chaque vol :
    - Extraction des informations de départ et d'arrivée
    - Récupération des codes IATA
    - Recherche des aéroports correspondants dans l'objet `World`
    - Création d'objets `Flight` et ajout à la liste

- Implémentation de la méthode `getList()` pour récupérer la liste des vols

**Tests :** La classe a été testée avec un fichier test.txt contenant une réponse JSON d'exemple pour l'aéroport d'Orly.

---

## Structure du projet

```
TP1_Java_3D/
├── src/
│   ├── Aeroport.java          # Classe représentant un aéroport
│   ├── World.java             # Classe contenant la liste des aéroports
│   ├── Flight.java            # Classe représentant un vol
│   ├── Earth.java             # Classe pour l'affichage 3D de la Terre
│   ├── Interface.java         # Classe principale de l'application JavaFX
│   └── JsonFlightFiller.java  # Classe pour parser les données JSON de l'API
├── data/
│   ├── airport-codes_no_comma.csv  # Fichier CSV des aéroports (à télécharger depuis Moodle)
│   ├── earth_texture.jpg           # Texture de la Terre (optionnel)
│   └── test.txt                    # Fichier de test JSON (optionnel)
├── README.md                       # Ce fichier
└── DataFlight_Project (13).pdf     # Sujet du TP
```

---

## Fonctionnalités implémentées

✅ **Classes de base** :
- Classe `Aeroport` avec calcul de distance
- Classe `World` avec lecture CSV et recherche d'aéroports

✅ **Interface 3D** :
- Affichage d'une sphère représentant la Terre
- Rotation automatique de la Terre
- Support de texture (si disponible)
- Caméra perspective avec zoom

✅ **Interactivité** :
- Zoom avec clic gauche et drag
- Clic droit pour trouver l'aéroport le plus proche
- Affichage de sphères colorées sur les aéroports

✅ **Intégration API** :
- Récupération des vols en temps réel via aviationstack.com
- Parsing JSON des données de vols
- Affichage des aéroports de départ en jaune

✅ **Gestion des threads** :
- Requêtes HTTP asynchrones pour ne pas bloquer l'interface
- Mise à jour de l'interface depuis les threads secondaires

---

## Configuration requise

- **Java** : Version 11 minimum (16 recommandé pour JavaFX)
- **JavaFX** : Bibliothèque JavaFX téléchargée et configurée dans le projet
- **Bibliothèque JSON** : `javax.json` (Oracle) ajoutée au projet
- **Fichiers de données** :
  - `data/airport-codes_no_comma.csv` : Fichier CSV des aéroports (disponible sur Moodle)
  - `data/earth_texture.jpg` : Texture de la Terre (optionnel, disponible sur Moodle)

---

## Instructions d'utilisation

1. **Préparation** :
   - Télécharger le fichier CSV des aéroports depuis Moodle et le placer dans le dossier `data/`
   - (Optionnel) Télécharger la texture de la Terre et la placer dans `data/earth_texture.jpg`
   - Configurer JavaFX dans IntelliJ (ajouter le répertoire `/lib` de JavaFX dans les bibliothèques)
   - Ajouter la bibliothèque JSON d'Oracle dans les dépendances du projet

2. **Configuration VM Options** (pour l'exécution) :
   ```
   --module-path "chemin/vers/javafx/lib" --add-modules javafx.controls,javafx.fxml
   ```

3. **Exécution** :
   - Lancer la méthode `main()` de la classe `Interface`
   - Une fenêtre 3D s'ouvre avec la Terre en rotation

4. **Utilisation** :
   - **Clic gauche + drag** : Zoomer/dézoomer
   - **Clic droit** : Trouver l'aéroport le plus proche et afficher les vols en direction

---

## Notes techniques

### Calcul de distance
La formule utilisée pour calculer la distance entre deux points GPS est une approximation simplifiée qui ne prend pas en compte la courbure réelle de la Terre. Pour une application réelle, il faudrait utiliser la formule de Haversine ou la formule de Vincenty.

### Conversion des coordonnées
La conversion des coordonnées 3D en latitude/longitude utilise les formules trigonométriques inverses pour une sphère. Cette méthode est précise pour une sphère parfaite.

### Gestion des erreurs
Le code inclut une gestion d'erreurs robuste pour :
- Les fichiers manquants ou mal formés
- Les erreurs de parsing JSON
- Les erreurs de connexion à l'API
- Les textures manquantes

---

## Améliorations possibles

- [ ] Utiliser la formule de Haversine pour un calcul de distance plus précis
- [ ] Ajouter une interface pour saisir manuellement un code IATA d'aéroport
- [ ] Afficher des informations détaillées sur les vols (compagnie, heure, etc.)
- [ ] Ajouter la possibilité de sauvegarder les résultats
- [ ] Améliorer l'interface utilisateur avec des contrôles supplémentaires
- [ ] Gérer la rotation manuelle de la Terre avec la souris
- [ ] Ajouter des animations lors de l'affichage des aéroports

---

## Conclusion

Ce projet a permis de mettre en pratique plusieurs concepts importants :
- **Programmation orientée objet** : Création de classes avec encapsulation et relations entre objets
- **Gestion de fichiers** : Lecture et parsing de fichiers CSV
- **Interface graphique 3D** : Utilisation de JavaFX pour créer une application interactive
- **APIs REST** : Intégration d'une API externe pour récupérer des données en temps réel
- **Parsing JSON** : Traitement de données structurées
- **Programmation asynchrone** : Gestion des threads pour éviter de bloquer l'interface

Le projet est fonctionnel et répond aux exigences du sujet du TP. Toutes les questions ont été traitées et implémentées.

---

**Auteurs :** Jean Jaussaud, Mael Korchef  
**Date :** 2024-2025  
**Enseignant :** Antoine Tauvel, ENSEA

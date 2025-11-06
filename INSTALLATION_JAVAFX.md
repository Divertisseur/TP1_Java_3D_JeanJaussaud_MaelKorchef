# Guide d'installation de JavaFX dans IntelliJ IDEA

## Étape 1 : Télécharger JavaFX

1. Allez sur le site officiel de JavaFX : https://openjfx.io/
2. Cliquez sur "Download" ou allez directement sur : https://openjfx.io/openjfx-docs/#install-javafx
3. Choisissez la version correspondant à votre système d'exploitation et votre architecture :
   - **Windows** : Téléchargez le SDK pour Windows
   - **Version Java** : Assurez-vous de télécharger une version compatible avec votre Java (Java 11, 16, 17, etc.)
4. Téléchargez le fichier ZIP (par exemple : `openjfx-17.0.2_windows-x64_bin-sdk.zip`)

## Étape 2 : Extraire JavaFX

1. Extrayez le fichier ZIP dans un répertoire facile à retrouver, par exemple :
   - `C:\javafx-sdk-17.0.2\` (ou la version que vous avez téléchargée)
2. **Important** : Notez le chemin complet vers le dossier `lib` à l'intérieur du SDK extrait :
   - Exemple : `C:\javafx-sdk-17.0.2\lib\`

## Étape 3 : Configurer JavaFX dans IntelliJ IDEA

### 3.1 Ajouter JavaFX comme bibliothèque externe

1. Ouvrez votre projet dans IntelliJ IDEA
2. Allez dans **File** → **Project Structure** (ou appuyez sur `Ctrl+Alt+Shift+S`)
3. Dans la fenêtre qui s'ouvre :
   - Cliquez sur **Libraries** dans le menu de gauche
   - Cliquez sur le bouton **+** (plus) en haut
   - Sélectionnez **Java**
4. Naviguez vers le dossier `lib` de JavaFX que vous avez extrait
   - Exemple : `C:\javafx-sdk-17.0.2\lib\`
   - Sélectionnez le dossier `lib` et cliquez sur **OK**
5. IntelliJ vous demandera à quel module ajouter cette bibliothèque :
   - Sélectionnez votre module (généralement le nom de votre projet)
   - Cliquez sur **OK**
6. Cliquez sur **Apply** puis **OK**

### 3.2 Configurer les VM Options pour l'exécution

1. Cliquez sur la flèche déroulante à côté du bouton "Run" en haut à droite d'IntelliJ
2. Sélectionnez **Edit Configurations...**
3. Si vous n'avez pas encore de configuration, créez-en une :
   - Cliquez sur **+** → **Application**
   - Nommez-la (par exemple : "Interface")
   - **Main class** : `Interface` (ou le nom de votre classe principale)
4. Dans la section **VM options**, ajoutez les options suivantes :
   ```
   --module-path "C:\javafx-sdk-17.0.2\lib" --add-modules javafx.controls,javafx.fxml
   ```
   **Important** : Remplacez `C:\javafx-sdk-17.0.2\lib` par le chemin réel vers votre dossier `lib` de JavaFX
5. Cliquez sur **Apply** puis **OK**

## Étape 4 : Vérifier l'installation

1. Ouvrez votre classe `Interface.java`
2. Les imports JavaFX ne devraient plus être en rouge :
   ```java
   import javafx.application.Application;
   import javafx.scene.Group;
   import javafx.scene.Scene;
   // etc.
   ```
3. Si les imports sont toujours en rouge :
   - Vérifiez que vous avez bien ajouté la bibliothèque dans Project Structure
   - Vérifiez que les VM options sont correctement configurées
   - Essayez de faire **File** → **Invalidate Caches / Restart...**

## Étape 5 : Exécuter l'application

1. Cliquez sur le bouton **Run** (ou appuyez sur `Shift+F10`)
2. Une fenêtre JavaFX devrait s'ouvrir avec votre application 3D

## Dépannage

### Erreur : "Error: JavaFX runtime components are missing"

**Solution** : Vérifiez que les VM options sont correctement configurées dans Run Configurations.

### Erreur : "Cannot resolve symbol 'javafx'"

**Solution** :
1. Vérifiez que la bibliothèque est bien ajoutée dans Project Structure → Libraries
2. Vérifiez que votre module utilise cette bibliothèque
3. Faites **File** → **Invalidate Caches / Restart...**

### Erreur : "Module not found: javafx.controls"

**Solution** : Vérifiez que le chemin dans les VM options est correct et pointe vers le dossier `lib` de JavaFX.

### Les imports sont corrects mais l'exécution échoue

**Solution** : Assurez-vous que :
- Les VM options incluent `--add-modules javafx.controls,javafx.fxml`
- Le chemin dans `--module-path` est correct (utilisez des guillemets si le chemin contient des espaces)

## Alternative : Utiliser Maven ou Gradle

Si vous préférez utiliser un gestionnaire de dépendances :

### Avec Maven

Ajoutez dans votre `pom.xml` :
```xml
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>17.0.2</version>
    </dependency>
</dependencies>
```

### Avec Gradle

Ajoutez dans votre `build.gradle` :
```gradle
dependencies {
    implementation 'org.openjfx:javafx-controls:17.0.2'
    implementation 'org.openjfx:javafx-fxml:17.0.2'
}
```

## Notes importantes

- **Version Java** : JavaFX 17 nécessite Java 11 minimum. Assurez-vous d'avoir une version compatible.
- **Chemin avec espaces** : Si votre chemin contient des espaces, utilisez des guillemets dans les VM options.
- **Version JavaFX** : Utilisez une version stable récente (17, 18, 19, 20, 21, etc.)

## Ressources utiles

- Site officiel JavaFX : https://openjfx.io/
- Documentation JavaFX : https://openjfx.io/openjfx-docs/
- Guide IntelliJ : https://www.jetbrains.com/help/idea/javafx.html


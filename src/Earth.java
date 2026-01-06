import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * Classe Earth qui hérite de Group
 * Elle contient la Terre (une sphère) et les représentations des aéroports
 */
public class Earth extends Group {
    private Sphere sph;
    private Rotate ry;

    /**
     * Constructeur de la classe Earth
     * Crée une sphère de rayon 300 pixels avec une texture de la Terre
     * Ajoute une rotation automatique autour de l'axe Y
     */
    public Earth() {
        // Création de la sphère de rayon 300
        sph = new Sphere(300);
        sph.setPickOnBounds(true); // S'assurer que la sphère est pickable pour les clics
        
        // Configuration de la rotation autour de l'axe Y
        ry = new Rotate(0, Rotate.Y_AXIS);
        this.getTransforms().add(ry);
        
        // Ajout de la sphère au groupe
        this.getChildren().add(sph);
        
        // Animation de rotation : un tour en 15 secondes
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long time) {
                // time est en nanosecondes, on veut un tour en 15 secondes = 15 * 10^9 nanosecondes
                // Un tour = 360 degrés
                double angle = (time / 1_000_000_000.0) * (360.0 / 15.0) % 360.0;
                ry.setAngle(angle);
            }
        };
        animationTimer.start();
    }

    /**
     * Définit la texture de la Terre
     * @param texturePath Le chemin vers l'image de texture (relatif ou absolu)
     */
    public void setTexture(String texturePath) {
        try {
            PhongMaterial material = new PhongMaterial();
            // Convertir le chemin en File puis en URL pour gérer les chemins relatifs
            java.io.File file = new java.io.File(texturePath);
            if (!file.exists()) {
                // Si le fichier n'existe pas, essayer avec le répertoire de travail actuel
                file = new java.io.File(System.getProperty("user.dir"), texturePath);
            }
            if (file.exists()) {
                String url = file.toURI().toURL().toString();
                material.setDiffuseMap(new javafx.scene.image.Image(url));
                sph.setMaterial(material);
                System.out.println("Texture chargée avec succès : " + file.getAbsolutePath());
            } else {
                throw new java.io.FileNotFoundException("Fichier de texture non trouvé : " + texturePath);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la texture : " + e.getMessage());
            // Matériau par défaut si la texture ne peut pas être chargée
            PhongMaterial defaultMaterial = new PhongMaterial();
            defaultMaterial.setDiffuseColor(Color.BLUE);
            sph.setMaterial(defaultMaterial);
        }
    }

    /**
     * Crée une sphère de couleur pour représenter un aéroport
     * @param airport L'aéroport à représenter
     * @param color La couleur de la sphère
     * @return La sphère créée
     */
    private Sphere createSphere(Aeroport airport, Color color) {
        double lat = Math.toRadians(airport.getLatitude());
        double lon = Math.toRadians(airport.getLongitude());
        double R = 300;
        
        double x = -R * Math.cos(lat) * Math.cos(lon);
        double y = -R * Math.sin(lat);
        double z = R * Math.cos(lat) * Math.sin(lon);
        
        // Créer une sphère plus grande et légèrement au-dessus de la surface
        Sphere sphere = new Sphere(8); // Augmenter la taille de 5 à 8
        // Positionner légèrement au-dessus de la surface pour qu'elle soit visible
        double scale = 1.02; // 2% au-dessus de la surface
        sphere.setTranslateX(x * scale);
        sphere.setTranslateY(y * scale);
        sphere.setTranslateZ(z * scale);
        
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(color);
        // Rendre le matériau émissif pour qu'il soit plus visible
        material.setSelfIlluminationMap(material.getDiffuseMap());
        sphere.setMaterial(material);
        
        return sphere;
    }

    /**
     * Affiche une sphère rouge sur l'aéroport donné
     * @param airport L'aéroport à afficher
     */
    public void displayRedSphere(Aeroport airport) {
        Sphere marker = createSphere(airport, Color.RED);
        this.getChildren().add(marker);
        System.out.println("Sphère rouge créée pour " + airport.getNom() + " à (" + 
            marker.getTranslateX() + ", " + marker.getTranslateY() + ", " + marker.getTranslateZ() + ")");
    }

    /**
     * Affiche une sphère jaune sur l'aéroport donné
     * @param airport L'aéroport à afficher
     */
    public void displayYellowSphere(Aeroport airport) {
        Sphere marker = createSphere(airport, Color.YELLOW);
        this.getChildren().add(marker);
    }

    /**
     * Retourne la sphère principale (la Terre)
     * @return La sphère de la Terre
     */
    public Sphere getSph() {
        return sph;
    }

    /**
     * Retourne la transformation de rotation de la Terre
     * @return La rotation autour de l'axe Y
     */
    public Rotate getRy() {
        return ry;
    }
}


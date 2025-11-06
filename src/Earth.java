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
     * @param a L'aéroport à représenter
     * @param color La couleur de la sphère
     * @return La sphère créée
     */
    public Sphere createSphere(Aeroport a, Color color) {
        Sphere sphere = new Sphere(5); // Augmenté à 5 pour une meilleure visibilité
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(color);
        sphere.setMaterial(material);
        
        // Conversion des coordonnées GPS en coordonnées 3D
        // Formule : X = R * cos(lat) * sin(lon)
        //           Y = -R * sin(lat)
        //           Z = -R * cos(lat) * cos(lon)
        // Avec R = 300 (rayon de la Terre)
        double R = 300.0;
        double latRad = Math.toRadians(a.getLatitude());
        double lonRad = Math.toRadians(a.getLongitude());
        
        double x = R * Math.cos(latRad) * Math.sin(lonRad);
        double y = -R * Math.sin(latRad);
        double z = -R * Math.cos(latRad) * Math.cos(lonRad);
        
        // Positionner la sphère légèrement au-dessus de la surface de la Terre pour qu'elle soit visible
        double scale = 1.02; // 2% au-dessus de la surface
        sphere.setTranslateX(x * scale);
        sphere.setTranslateY(y * scale);
        sphere.setTranslateZ(z * scale);
        
        // S'assurer que la sphère est pickable
        sphere.setPickOnBounds(true);
        
        return sphere;
    }

    /**
     * Affiche une sphère rouge sur l'aéroport donné
     * @param a L'aéroport à afficher
     */
    public void displayRedSphere(Aeroport a) {
        if (a != null) {
            Sphere redSphere = createSphere(a, Color.RED);
            this.getChildren().add(redSphere);
            System.out.println("Sphère rouge créée et ajoutée au groupe. Nombre d'enfants: " + this.getChildren().size());
            System.out.println("Position de la sphère: (" + redSphere.getTranslateX() + ", " + redSphere.getTranslateY() + ", " + redSphere.getTranslateZ() + ")");
        }
    }

    /**
     * Affiche une sphère jaune sur l'aéroport donné
     * @param a L'aéroport à afficher
     */
    public void displayYellowSphere(Aeroport a) {
        if (a != null) {
            Sphere yellowSphere = createSphere(a, Color.YELLOW);
            this.getChildren().add(yellowSphere);
        }
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


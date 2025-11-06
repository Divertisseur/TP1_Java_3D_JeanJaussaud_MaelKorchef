import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Classe Interface contenant l'interface du projet
 * Elle gère l'interactivité et l'affichage 3D
 */
public class Interface extends Application {
    private World w;
    private Earth earth;
    private PerspectiveCamera camera;
    private double mouseX = 0;
    private double mouseY = 0;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("DataFlight - Catch me if you can!");
        
        // Chargement de la liste des aéroports
        w = new World("./data/airport-codes_no_comma.csv");
        System.out.println("Chargé " + w.getList().size() + " aéroports.");
        
        // Création de l'objet Earth
        earth = new Earth();
        
        // Tentative de chargement de la texture (si disponible)
        try {
            earth.setTexture("data/earth_lights_4800.png");
        } catch (Exception e) {
            System.out.println("Texture non trouvée, utilisation de la couleur par défaut");
            e.printStackTrace();
        }
        
        Pane pane = new Pane(earth);
        Scene theScene = new Scene(pane, 1200, 800, true);
        
        // Configuration de la caméra
        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-1000);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(35);
        theScene.setCamera(camera);
        
        // Gestion des événements de la souris
        theScene.addEventHandler(MouseEvent.ANY, event -> {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
                mouseX = event.getSceneX();
                mouseY = event.getSceneY();
                System.out.println("Clicked on : (" + event.getSceneX() + ", " + event.getSceneY() + ")");
            }
            
            if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                // Zoom en déplaçant la caméra sur l'axe Z
                double deltaY = event.getSceneY() - mouseY;
                double currentZ = camera.getTranslateZ();
                double newZ = currentZ + deltaY * 2; // Facteur de zoom
                
                // Limiter le zoom
                if (newZ < -2000) newZ = -2000;
                if (newZ > -300) newZ = -300;
                
                camera.setTranslateZ(newZ);
                mouseY = event.getSceneY();
            }
            
            // Gestion du clic droit pour trouver l'aéroport le plus proche
            if (event.getButton() == MouseButton.SECONDARY && event.getEventType() == MouseEvent.MOUSE_CLICKED) {
                System.out.println("Clic droit détecté !");
                
                // Essayer d'abord d'utiliser PickResult pour obtenir les coordonnées locales de la sphère
                PickResult pickResult = event.getPickResult();
                javafx.scene.Node intersectedNode = pickResult.getIntersectedNode();
                
                // Vérifier si on a cliqué sur la sphère de la Terre
                if (intersectedNode == earth.getSph() || (intersectedNode != null && intersectedNode.getParent() == earth)) {
                    // Utiliser les coordonnées locales de la sphère (déjà dans le système sans rotation)
                    javafx.geometry.Point3D localPoint = pickResult.getIntersectedPoint();
                    System.out.println("Point d'intersection local (sphère): (" + localPoint.getX() + ", " + localPoint.getY() + ", " + localPoint.getZ() + ")");
                    
                    double R = 300.0;
                    double x = localPoint.getX();
                    double y = localPoint.getY();
                    double z = localPoint.getZ();
                    
                    // Normaliser pour s'assurer qu'on est sur la sphère
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist > 0) {
                        x = (x / dist) * R;
                        y = (y / dist) * R;
                        z = (z / dist) * R;
                    }
                    
                    // Conversion directe en GPS (pas besoin d'inverser la rotation car on est déjà dans le système local)
                    double latitude = Math.toDegrees(-Math.asin(y / R));
                    double longitude = Math.toDegrees(Math.atan2(x, -z));
                    
                    System.out.println("Coordonnées GPS calculées (méthode PickResult): latitude=" + latitude + ", longitude=" + longitude);
                    
                    // Recherche de l'aéroport le plus proche
                    Aeroport nearest = w.findNearestAirport(longitude, latitude);
                    if (nearest != null) {
                        System.out.println("Aéroport le plus proche trouvé : " + nearest);
                        System.out.println("  - Code IATA: " + nearest.getCodeIATA());
                        System.out.println("  - Nom: " + nearest.getNom());
                        System.out.println("  - Position GPS: lat=" + nearest.getLatitude() + ", lon=" + nearest.getLongitude());
                        
                        // Affichage de la boule rouge sur l'aéroport
                        earth.displayRedSphere(nearest);
                        System.out.println("Sphère rouge ajoutée pour l'aéroport " + nearest.getCodeIATA());
                        
                        // Récupération des vols en direction de cet aéroport via l'API
                        fetchFlightsForAirport(nearest);
                    } else {
                        System.out.println("Aucun aéroport trouvé !");
                    }
                    return; // Sortir si on a utilisé PickResult avec succès
                }
                
                // Sinon, utiliser la méthode de projection manuelle
                System.out.println("PickResult n'a pas trouvé la sphère, utilisation de la projection manuelle");
                
                // Utiliser les coordonnées 2D de la souris pour projeter un rayon depuis la caméra
                double mouseX = event.getSceneX();
                double mouseY = event.getSceneY();
                double sceneWidth = theScene.getWidth();
                double sceneHeight = theScene.getHeight();
                
                System.out.println("Position souris: (" + mouseX + ", " + mouseY + ")");
                System.out.println("Taille scène: " + sceneWidth + " x " + sceneHeight);
                
                // Convertir les coordonnées de la souris en coordonnées normalisées (-1 à 1)
                double normalizedX = (mouseX / sceneWidth) * 2.0 - 1.0;
                double normalizedY = 1.0 - (mouseY / sceneHeight) * 2.0; // Inverser Y
                
                System.out.println("Coordonnées normalisées: (" + normalizedX + ", " + normalizedY + ")");
                
                // Obtenir les paramètres de la caméra
                double cameraZ = camera.getTranslateZ();
                double fieldOfView = camera.getFieldOfView();
                double aspectRatio = sceneWidth / sceneHeight;
                
                System.out.println("Caméra Z: " + cameraZ + ", FOV: " + fieldOfView + ", Aspect: " + aspectRatio);
                
                // Calculer la distance focale basée sur le field of view
                double fovRad = Math.toRadians(fieldOfView);
                double focalLength = 1.0 / Math.tan(fovRad / 2.0);
                
                // Direction du rayon depuis la caméra (qui est à (0, 0, cameraZ))
                // Prendre en compte l'aspect ratio pour la projection correcte
                double rayDirX = (normalizedX / focalLength) * aspectRatio;
                double rayDirY = normalizedY / focalLength;
                double rayDirZ = -1.0; // Vers l'avant (négatif Z)
                
                // Normaliser la direction du rayon
                double rayLength = Math.sqrt(rayDirX*rayDirX + rayDirY*rayDirY + rayDirZ*rayDirZ);
                if (rayLength > 0) {
                    rayDirX /= rayLength;
                    rayDirY /= rayLength;
                    rayDirZ /= rayLength;
                }
                
                // Intersection du rayon avec la sphère (centre à (0,0,0), rayon R=300)
                double R = 300.0;
                // La caméra peut avoir des translations X et Y
                double cameraX = camera.getTranslateX();
                double cameraY = camera.getTranslateY();
                double cameraZPos = cameraZ;
                
                System.out.println("Position caméra: (" + cameraX + ", " + cameraY + ", " + cameraZPos + ")");
                
                // Équation : ||camera + t*rayDir|| = R
                // Résoudre pour t : t² + 2*(camera·rayDir)*t + (||camera||² - R²) = 0
                double dot = cameraX*rayDirX + cameraY*rayDirY + cameraZPos*rayDirZ;
                double cameraDistSq = cameraX*cameraX + cameraY*cameraY + cameraZPos*cameraZPos;
                double discriminant = dot*dot - (cameraDistSq - R*R);
                
                if (discriminant >= 0) {
                    // Prendre la solution la plus proche (t positif le plus petit)
                    double t = -dot - Math.sqrt(discriminant);
                    if (t < 0) t = -dot + Math.sqrt(discriminant);
                    
                    // Point d'intersection sur la sphère (dans le système de la scène)
                    double x = cameraX + t * rayDirX;
                    double y = cameraY + t * rayDirY;
                    double z = cameraZPos + t * rayDirZ;
                    
                    System.out.println("Point d'intersection 3D calculé: (" + x + ", " + y + ", " + z + ")");
                    
                    // Normaliser le point pour s'assurer qu'il est sur la sphère
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist > 0) {
                        x = (x / dist) * R;
                        y = (y / dist) * R;
                        z = (z / dist) * R;
                    }
                    
                    // Obtenir l'angle de rotation actuel de la Terre
                    double rotationAngle = earth.getRy().getAngle();
                    double rotationRad = Math.toRadians(rotationAngle);
                    
                    System.out.println("Angle de rotation de la Terre: " + rotationAngle + " degrés");
                    
                    // Inverser la rotation autour de l'axe Y pour obtenir les coordonnées dans le système local
                    // La rotation est appliquée au groupe Earth, donc nous devons l'inverser
                    // Si la rotation transforme (x,z) en (x',z'), alors :
                    // x' = x*cos(θ) - z*sin(θ)
                    // z' = x*sin(θ) + z*cos(θ)
                    // Pour inverser, on fait :
                    // x = x'*cos(θ) + z'*sin(θ)
                    // z = -x'*sin(θ) + z'*cos(θ)
                    // Mais comme la rotation est appliquée dans le sens positif, on inverse avec -θ
                    double cosRot = Math.cos(-rotationRad);
                    double sinRot = Math.sin(-rotationRad);
                    double xLocal = x * cosRot - z * sinRot;
                    double zLocal = x * sinRot + z * cosRot;
                    
                    System.out.println("Point après inversion rotation: (" + xLocal + ", " + y + ", " + zLocal + ")");
                    
                    // Calcul de la latitude : lat = -arcsin(y / R)
                    // y est déjà normalisé, donc y/R devrait être entre -1 et 1
                    double latitude = Math.toDegrees(-Math.asin(y / R));
                    
                    // Calcul de la longitude : lon = atan2(x, -z)
                    // Note: atan2(x, -z) donne la longitude correcte
                    double longitude = Math.toDegrees(Math.atan2(xLocal, -zLocal));
                    
                    System.out.println("Coordonnées GPS calculées: latitude=" + latitude + ", longitude=" + longitude);
                    
                    // Vérifier que les coordonnées sont valides
                    if (Double.isNaN(latitude) || Double.isNaN(longitude) || 
                        Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
                        System.out.println("ERREUR: Coordonnées GPS invalides !");
                        return;
                    }
                    
                    // Recherche de l'aéroport le plus proche
                    Aeroport nearest = w.findNearestAirport(longitude, latitude);
                    if (nearest != null) {
                        System.out.println("Aéroport le plus proche trouvé : " + nearest);
                        System.out.println("  - Code IATA: " + nearest.getCodeIATA());
                        System.out.println("  - Nom: " + nearest.getNom());
                        System.out.println("  - Position GPS: lat=" + nearest.getLatitude() + ", lon=" + nearest.getLongitude());
                        
                        // Affichage de la boule rouge sur l'aéroport
                        earth.displayRedSphere(nearest);
                        System.out.println("Sphère rouge ajoutée pour l'aéroport " + nearest.getCodeIATA());
                        
                        // Récupération des vols en direction de cet aéroport via l'API
                        fetchFlightsForAirport(nearest);
                    } else {
                        System.out.println("Aucun aéroport trouvé !");
                    }
                } else {
                    System.out.println("Aucune intersection avec la sphère (clic peut-être en dehors de la Terre)");
                }
            }
        });
        
        primaryStage.setScene(theScene);
        primaryStage.show();
    }

    /**
     * Récupère les vols en direction d'un aéroport via l'API aviationstack
     * @param airport L'aéroport de destination
     */
    private void fetchFlightsForAirport(Aeroport airport) {
        // Exécution dans un thread séparé pour éviter de bloquer l'interface
        CompletableFuture.runAsync(() -> {
            try {
                String apiKey = "cfaf27d3b7c76c08bafee49ddb0df72c"; // Clé d'exemple, à remplacer par la vôtre
                String url = "http://api.aviationstack.com/v1/flights?access_key=" + apiKey + "&arr_iata=" + airport.getCodeIATA();
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    String jsonResponse = response.body();
                    JsonFlightFiller filler = new JsonFlightFiller(jsonResponse, w);
                    
                    // Affichage des aéroports de départ en jaune
                    javafx.application.Platform.runLater(() -> {
                        for (Flight flight : filler.getList()) {
                            if (flight.getDepart() != null) {
                                earth.displayYellowSphere(flight.getDepart());
                            }
                        }
                        System.out.println("Affiché " + filler.getList().size() + " vols vers " + airport.getCodeIATA());
                    });
                } else {
                    System.err.println("Erreur API : " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des vols : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}


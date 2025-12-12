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
                
                // Vérifier si on a cliqué sur la sphère de la Terre ou une sphère d'aéroport
                if (intersectedNode == earth.getSph() || (intersectedNode != null && intersectedNode.getParent() == earth)) {
                    // Utiliser les coordonnées locales de la sphère
                    // PickResult retourne les coordonnées dans le système local de la sphère (sans rotation)
                    javafx.geometry.Point3D localPoint = pickResult.getIntersectedPoint();
                    
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
                    
                    // Conversion directe en GPS (PickResult donne déjà les coordonnées dans le système local sans rotation)
                    double latitude = Math.toDegrees(-Math.asin(y / R));
                    double longitude = Math.toDegrees(Math.atan2(x, -z));
                    
                    // Vérifier que les coordonnées sont valides
                    if (Double.isNaN(latitude) || Double.isNaN(longitude) || 
                        Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
                        System.out.println("ERREUR: Coordonnées GPS invalides !");
                        return;
                    }
                    
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
                        
                        // Récupération des vols en direction de cet aéroport via l'API
                        fetchFlightsForAirport(nearest);
                    } else {
                        System.out.println("Aucun aéroport trouvé !");
                    }
                    return; // Sortir si on a utilisé PickResult avec succès
                }
                
                // Sinon, utiliser la méthode de projection manuelle
                // Utiliser les coordonnées 2D de la souris pour projeter un rayon depuis la caméra
                double mouseX = event.getSceneX();
                double mouseY = event.getSceneY();
                double sceneWidth = theScene.getWidth();
                double sceneHeight = theScene.getHeight();
                
                // Convertir les coordonnées de la souris en coordonnées normalisées (-1 à 1)
                double normalizedX = (mouseX / sceneWidth) * 2.0 - 1.0;
                double normalizedY = 1.0 - (mouseY / sceneHeight) * 2.0; // Inverser Y
                
                // Obtenir les paramètres de la caméra
                double cameraZ = camera.getTranslateZ();
                double fieldOfView = camera.getFieldOfView();
                double aspectRatio = sceneWidth / sceneHeight;
                
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
                double cameraX = camera.getTranslateX();
                double cameraY = camera.getTranslateY();
                double cameraZPos = cameraZ;
                
                // Équation : ||camera + t*rayDir|| = R
                // Résoudre pour t : t² + 2*(camera·rayDir)*t + (||camera||² - R²) = 0
                double dot = cameraX*rayDirX + cameraY*rayDirY + cameraZPos*rayDirZ;
                double cameraDistSq = cameraX*cameraX + cameraY*cameraY + cameraZPos*cameraZPos;
                double discriminant = dot*dot - (cameraDistSq - R*R);
                
                if (discriminant >= 0) {
                    // Prendre la solution la plus proche (t positif le plus petit)
                    double t = -dot - Math.sqrt(discriminant);
                    if (t < 0) t = -dot + Math.sqrt(discriminant);
                    
                    // Point d'intersection sur la sphère (dans le système de la scène avec rotation)
                    double x = cameraX + t * rayDirX;
                    double y = cameraY + t * rayDirY;
                    double z = cameraZPos + t * rayDirZ;
                    
                    // Normaliser le point pour s'assurer qu'il est sur la sphère
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist > 0) {
                        x = (x / dist) * R;
                        y = (y / dist) * R;
                        z = (z / dist) * R;
                    }
                    
                    // IMPORTANT: Inverser la rotation de la Terre pour obtenir les coordonnées dans le système local
                    // La Terre tourne autour de l'axe Y, donc on doit appliquer une rotation inverse
                    double rotationAngle = earth.getRy().getAngle();
                    double rotationRad = Math.toRadians(rotationAngle);
                    double cosRot = Math.cos(rotationRad);
                    double sinRot = Math.sin(rotationRad);
                    
                    // Inversion de la rotation autour de Y: rotation de -angle
                    // x_local = x * cos(-θ) - z * sin(-θ) = x * cos(θ) + z * sin(θ)
                    // z_local = x * sin(-θ) + z * cos(-θ) = -x * sin(θ) + z * cos(θ)
                    double xLocal = x * cosRot + z * sinRot;
                    double zLocal = -x * sinRot + z * cosRot;
                    
                    // Conversion des coordonnées 3D locales en GPS
                    // Formule inverse de createSphere:
                    // X = R * cos(lat) * sin(lon)  =>  sin(lon) = X / (R * cos(lat))
                    // Y = -R * sin(lat)  =>  lat = -arcsin(Y / R)
                    // Z = -R * cos(lat) * cos(lon)  =>  cos(lon) = -Z / (R * cos(lat))
                    double latitude = Math.toDegrees(-Math.asin(y / R));
                    double longitude = Math.toDegrees(Math.atan2(xLocal, -zLocal));
                    
                    // Vérifier que les coordonnées sont valides
                    if (Double.isNaN(latitude) || Double.isNaN(longitude) || 
                        Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
                        System.out.println("ERREUR: Coordonnées GPS invalides !");
                        return;
                    }
                    
                    System.out.println("Coordonnées GPS calculées: latitude=" + latitude + ", longitude=" + longitude);
                    
                    // Recherche de l'aéroport le plus proche
                    Aeroport nearest = w.findNearestAirport(longitude, latitude);
                    if (nearest != null) {
                        System.out.println("Aéroport le plus proche trouvé : " + nearest);
                        System.out.println("  - Code IATA: " + nearest.getCodeIATA());
                        System.out.println("  - Nom: " + nearest.getNom());
                        System.out.println("  - Position GPS: lat=" + nearest.getLatitude() + ", lon=" + nearest.getLongitude());
                        
                        // Affichage de la boule rouge sur l'aéroport
                        earth.displayRedSphere(nearest);
                        
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

    private long lastApiCallTime = 0;
    private static final long MIN_API_DELAY_MS = 1000; // Délai minimum de 1 seconde entre les appels API
    
    /**
     * Récupère les vols en direction d'un aéroport via l'API aviationstack
     * @param airport L'aéroport de destination
     */
    private void fetchFlightsForAirport(Aeroport airport) {
        // Exécution dans un thread séparé pour éviter de bloquer l'interface
        CompletableFuture.runAsync(() -> {
            try {
                // Attendre un délai minimum entre les appels API pour éviter l'erreur 429
                long currentTime = System.currentTimeMillis();
                long timeSinceLastCall = currentTime - lastApiCallTime;
                if (timeSinceLastCall < MIN_API_DELAY_MS) {
                    try {
                        Thread.sleep(MIN_API_DELAY_MS - timeSinceLastCall);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                lastApiCallTime = System.currentTimeMillis();
                
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
                } else if (response.statusCode() == 429) {
                    System.err.println("Erreur 429: Trop de requêtes. Veuillez attendre avant de cliquer à nouveau.");
                    javafx.application.Platform.runLater(() -> {
                        System.out.println("Limite de taux d'API atteinte. Attendez quelques secondes avant de réessayer.");
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


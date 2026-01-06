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
                
                // Utiliser PickResult pour obtenir le point d'intersection
                PickResult pickResult = event.getPickResult();
                javafx.scene.Node intersectedNode = pickResult.getIntersectedNode();
                
                // Vérifier si on a cliqué sur la sphère de la Terre ou un de ses enfants (marqueurs d'aéroports)
                boolean isOnEarth = (intersectedNode == earth.getSph()) || 
                                   (intersectedNode != null && earth.getChildren().contains(intersectedNode));
                
                javafx.geometry.Point3D localPoint = null;
                
                if (isOnEarth && intersectedNode == earth.getSph()) {
                    // Clic direct sur la sphère - utiliser les coordonnées locales
                    localPoint = pickResult.getIntersectedPoint();
                } else if (isOnEarth) {
                    // Clic sur un marqueur d'aéroport - utiliser les coordonnées de la scène et convertir
                    javafx.geometry.Point3D scenePoint = pickResult.getIntersectedPoint();
                    // Convertir vers le système local de Earth en inversant la rotation
                    double rotationAngle = earth.getRy().getAngle();
                    double rotationRad = Math.toRadians(rotationAngle);
                    double cosRot = Math.cos(rotationRad);
                    double sinRot = Math.sin(rotationRad);
                    
                    // Les coordonnées de scène doivent être converties en coordonnées locales
                    // On projette le point sur la sphère
                    double R = 300.0;
                    double x = scenePoint.getX();
                    double y = scenePoint.getY();
                    double z = scenePoint.getZ();
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist > 0.001) {
                        x = (x / dist) * R;
                        y = (y / dist) * R;
                        z = (z / dist) * R;
                    }
                    // Inverser la rotation pour obtenir les coordonnées locales
                    // (rotationAngle, rotationRad, cosRot, sinRot déjà définis plus haut)
                    // Formule inversée (échanger les signes pour corriger le problème de côté)
                    double xLocal = x * cosRot - z * sinRot;
                    double zLocal = x * sinRot + z * cosRot;
                    localPoint = new javafx.geometry.Point3D(xLocal, y, zLocal);
                } else {
                    // Clic sur le Pane ou ailleurs - utiliser ray-casting pour trouver l'intersection avec la Terre
                    double mouseX = event.getSceneX();
                    double mouseY = event.getSceneY();
                    double sceneWidth = theScene.getWidth();
                    double sceneHeight = theScene.getHeight();
                    
                    // Convertir les coordonnées de la souris en coordonnées normalisées (-1 à 1)
                    double normalizedX = (mouseX / sceneWidth) * 2.0 - 1.0;
                    double normalizedY = 1.0 - (mouseY / sceneHeight) * 2.0;
                    
                    // Paramètres de la caméra
                    double cameraZ = camera.getTranslateZ();
                    double fieldOfView = camera.getFieldOfView();
                    double aspectRatio = sceneWidth / sceneHeight;
                    
                    // Calculer la distance focale
                    double fovRad = Math.toRadians(fieldOfView);
                    double focalLength = 1.0 / Math.tan(fovRad / 2.0);
                    
                    // Direction du rayon depuis la caméra
                    double rayDirX = (normalizedX / focalLength) * aspectRatio;
                    double rayDirY = normalizedY / focalLength;
                    double rayDirZ = -1.0;
                    
                    // Normaliser
                    double rayLength = Math.sqrt(rayDirX*rayDirX + rayDirY*rayDirY + rayDirZ*rayDirZ);
                    if (rayLength > 0) {
                        rayDirX /= rayLength;
                        rayDirY /= rayLength;
                        rayDirZ /= rayLength;
                    }
                    
                    // Intersection avec la sphère (centre à (0,0,0), rayon R=300)
                    double R = 300.0;
                    double cameraX = camera.getTranslateX();
                    double cameraY = camera.getTranslateY();
                    double cameraZPos = cameraZ;
                    
                    // Équation : ||camera + t*rayDir|| = R
                    double dot = cameraX*rayDirX + cameraY*rayDirY + cameraZPos*rayDirZ;
                    double cameraDistSq = cameraX*cameraX + cameraY*cameraY + cameraZPos*cameraZPos;
                    double discriminant = dot*dot - (cameraDistSq - R*R);
                    
                    if (discriminant >= 0) {
                        double t = -dot - Math.sqrt(discriminant);
                        if (t < 0) t = -dot + Math.sqrt(discriminant);
                        
                        // Point d'intersection dans le système de la scène
                        double xScene = cameraX + t * rayDirX;
                        double yScene = cameraY + t * rayDirY;
                        double zScene = cameraZPos + t * rayDirZ;
                        
                        // Normaliser
                        double dist = Math.sqrt(xScene*xScene + yScene*yScene + zScene*zScene);
                        if (dist > 0.001) {
                            xScene = (xScene / dist) * R;
                            yScene = (yScene / dist) * R;
                            zScene = (zScene / dist) * R;
                        }
                        
                        // Inverser la rotation de la Terre pour obtenir les coordonnées locales
                        // La rotation est autour de l'axe Y
                        // En JavaFX, une rotation positive autour de Y transforme:
                        // x' = x*cos(θ) - z*sin(θ)
                        // z' = x*sin(θ) + z*cos(θ)
                        // Pour inverser (retourner au système local), on utilise l'angle négatif:
                        // x = x'*cos(-θ) - z'*sin(-θ) = x'*cos(θ) + z'*sin(θ)
                        // z = x'*sin(-θ) + z'*cos(-θ) = -x'*sin(θ) + z'*cos(θ)
                        // Mais si les points apparaissent du mauvais côté, essayons l'inverse
                        double rotationAngle = earth.getRy().getAngle();
                        double rotationRad = Math.toRadians(rotationAngle);
                        double cosRot = Math.cos(rotationRad);
                        double sinRot = Math.sin(rotationRad);
                        
                        // Formule inversée (échanger les signes pour corriger le problème de côté)
                        double xLocal = xScene * cosRot - zScene * sinRot;
                        double zLocal = xScene * sinRot + zScene * cosRot;
                        
                        System.out.println("Point scène: (" + String.format("%.1f", xScene) + ", " + String.format("%.1f", yScene) + ", " + String.format("%.1f", zScene) + ")");
                        System.out.println("Angle rotation: " + String.format("%.2f", rotationAngle) + "°");
                        System.out.println("Point local (après inversion): (" + String.format("%.1f", xLocal) + ", " + String.format("%.1f", yScene) + ", " + String.format("%.1f", zLocal) + ")");
                        
                        localPoint = new javafx.geometry.Point3D(xLocal, yScene, zLocal);
                    } else {
                        System.out.println("Aucune intersection avec la sphère");
                        return;
                    }
                }
                
                if (localPoint != null) {
                    double R = 300.0;
                    double x = localPoint.getX();
                    double y = localPoint.getY();
                    double z = localPoint.getZ();
                    
                    // Normaliser pour s'assurer qu'on est exactement sur la sphère
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist > 0.001) {
                        // Normaliser à R exactement
                        x = (x / dist) * R;
                        y = (y / dist) * R;
                        z = (z / dist) * R;
                    } else {
                        System.out.println("Point trop proche du centre");
                        return;
                    }
                    
                    // Conversion en GPS (formule inverse de createSphere)
                    // x = -R * cos(lat) * cos(lon)
                    // y = -R * sin(lat)
                    // z = R * cos(lat) * sin(lon)
                    // Donc:
                    // lat = -arcsin(y / R)
                    // lon = atan2(z, -x)
                    double latitude = Math.toDegrees(-Math.asin(y / R));
                    double longitude = Math.toDegrees(Math.atan2(z, -x));
                    
                    // Vérifier que les coordonnées sont valides
                    if (Double.isNaN(latitude) || Double.isNaN(longitude) || 
                        Double.isInfinite(latitude) || Double.isInfinite(longitude) ||
                        Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                        System.out.println("ERREUR: Coordonnées GPS invalides ! lat=" + latitude + ", lon=" + longitude);
                        return;
                    }
                    
                    System.out.println("GPS: lat=" + String.format("%.4f", latitude) + "°, lon=" + String.format("%.4f", longitude) + "°");
                    
                    // Recherche de l'aéroport le plus proche
                    Aeroport nearest = w.findNearestAirport(longitude, latitude);
                    if (nearest != null) {
                        System.out.println("Aéroport trouvé: " + nearest.getNom() + " (" + nearest.getCodeIATA() + ")");
                        System.out.println("  Position aéroport: lat=" + nearest.getLatitude() + "°, lon=" + nearest.getLongitude() + "°");
                        System.out.println("  Distance depuis le clic: lat_diff=" + (nearest.getLatitude() - latitude) + "°, lon_diff=" + (nearest.getLongitude() - longitude) + "°");
                        earth.displayRedSphere(nearest);
                        System.out.println("Sphère rouge ajoutée. Nombre d'enfants de Earth: " + earth.getChildren().size());
                        fetchFlightsForAirport(nearest);
                    } else {
                        System.out.println("Aucun aéroport trouvé");
                    }
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


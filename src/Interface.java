import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Translate;
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
                PickResult pickResult = event.getPickResult();
                if (pickResult.getIntersectedNode() != null) {
                    // Récupération du point d'intersection en coordonnées 3D
                    javafx.geometry.Point3D point = pickResult.getIntersectedPoint();
                    
                    // Conversion des coordonnées 3D en latitude et longitude
                    // Pour une sphère de rayon R = 300 :
                    // x = R * cos(lat) * sin(lon)
                    // y = -R * sin(lat)
                    // z = -R * cos(lat) * cos(lon)
                    double R = 300.0;
                    double x = point.getX();
                    double y = point.getY();
                    double z = point.getZ();
                    
                    // Calcul de la latitude : lat = -arcsin(y / R)
                    double latitude = Math.toDegrees(-Math.asin(y / R));
                    
                    // Calcul de la longitude : lon = atan2(x, -z)
                    double longitude = Math.toDegrees(Math.atan2(x, -z));
                    
                    // Recherche de l'aéroport le plus proche
                    Aeroport nearest = w.findNearestAirport(longitude, latitude);
                    if (nearest != null) {
                        System.out.println("Aéroport le plus proche : " + nearest);
                        
                        // Affichage de la boule rouge sur l'aéroport
                        earth.displayRedSphere(nearest);
                        
                        // Récupération des vols en direction de cet aéroport via l'API
                        fetchFlightsForAirport(nearest);
                    }
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


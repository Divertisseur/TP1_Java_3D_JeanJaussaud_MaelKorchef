import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Classe JsonFlightFiller utilisant le paquet d'analyse JSON d'Oracle
 * pour interroger une base en ligne et récupérer tous les vols
 */
public class JsonFlightFiller {
    private ArrayList<Flight> list = new ArrayList<>();

    /**
     * Constructeur qui parse une chaîne JSON et crée la liste des vols
     * @param jsonString La chaîne JSON contenant les données de vols
     * @param w L'objet World contenant les aéroports
     */
    public JsonFlightFiller(String jsonString, World w) {
        try {
            InputStream is = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
            JsonReader rdr = Json.createReader(is);
            JsonObject obj = rdr.readObject();
            JsonArray results = obj.getJsonArray("data");
            
            for (JsonObject result : results.getValuesAs(JsonObject.class)) {
                try {
                    // Extraction des informations du vol depuis le JSON
                    // Structure typique : {"flight": {"departure": {"iata": "CDG"}, "arrival": {"iata": "JFK"}}}
                    JsonObject flight = result.getJsonObject("flight");
                    if (flight != null) {
                        JsonObject departure = flight.getJsonObject("departure");
                        JsonObject arrival = flight.getJsonObject("arrival");
                        
                        if (departure != null && arrival != null) {
                            String depIATA = departure.getString("iata", "");
                            String arrIATA = arrival.getString("iata", "");
                            
                            if (!depIATA.isEmpty() && !arrIATA.isEmpty()) {
                                Aeroport dep = w.findByCode(depIATA);
                                Aeroport arr = w.findByCode(arrIATA);
                                
                                if (dep != null && arr != null) {
                                    list.add(new Flight(dep, arr));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retourne la liste des vols
     * @return La liste des vols
     */
    public ArrayList<Flight> getList() {
        return list;
    }

    /**
     * Méthode main pour tester la classe JsonFlightFiller
     */
    public static void main(String[] args) {
        try {
            World w = new World("./data/airport-codes_no_comma.csv");
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("data/test.txt"));
            String test = br.readLine();
            JsonFlightFiller jsonFlightFiller = new JsonFlightFiller(test, w);
            System.out.println("Nombre de vols trouvés : " + jsonFlightFiller.getList().size());
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


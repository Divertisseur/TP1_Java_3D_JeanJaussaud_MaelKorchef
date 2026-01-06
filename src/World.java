import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe World contenant la liste de tous les aéroports du monde
 * Elle est créée par la lecture d'un fichier CSV
 */
public class World {
    private List<Aeroport> list;

    /**
     * Parse une ligne CSV en respectant les champs entre guillemets
     * @param line La ligne CSV à parser
     * @return Un tableau de champs
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // Toggle l'état "inside quotes"
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                // Fin d'un champ (seulement si on n'est pas dans des guillemets)
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                // Ajouter le caractère au champ actuel
                currentField.append(c);
            }
        }
        // Ajouter le dernier champ
        fields.add(currentField.toString().trim());
        
        return fields.toArray(new String[0]);
    }

    /**
     * Constructeur qui lit le fichier CSV et crée la liste des aéroports
     * @param fileName Le chemin vers le fichier CSV contenant les aéroports
     */
    public World(String fileName) {
        list = new ArrayList<>();
        try {
            BufferedReader buf = new BufferedReader(new FileReader(fileName));
            String s = buf.readLine(); // Lire l'en-tête
            s = buf.readLine(); // Lire la première ligne de données
            while (s != null) {
                // Parser la ligne CSV en respectant les champs entre guillemets
                String fields[] = parseCSVLine(s);
                
                // Charger tous les aéroports (pas seulement large_airport)
                // Exclure seulement les aéroports fermés
                if (fields.length > 1 && !fields[1].equals("closed")) {
                    try {
                        // Le format du CSV est : ident,type,name,elevation_ft,continent,iso_country,iso_region,municipality,gps_code,iata_code,local_code,coordinates,,
                        // Les coordonnées GPS sont dans le champ 11 (index 11) avec format "longitude, latitude"
                        String nom = fields.length > 2 ? fields[2] : "";
                        String codeIATA = "";
                        double latitude = 0.0;
                        double longitude = 0.0;
                        
                        // Chercher le code IATA (dans le champ 9, index 9)
                        if (fields.length > 9 && !fields[9].isEmpty()) {
                            codeIATA = fields[9];
                        }
                        
                        // Chercher les coordonnées GPS dans le champ 11 (index 11)
                        // Format : "longitude, latitude" ou longitude, latitude (sans guillemets après parsing)
                        if (fields.length > 11 && !fields[11].isEmpty()) {
                            String coords = fields[11].replaceAll("\"", "").trim();
                            // Parser les coordonnées (format: "longitude, latitude" ou longitude, latitude)
                            String[] coordParts = coords.split(",");
                            if (coordParts.length == 2) {
                                try {
                                    longitude = Double.parseDouble(coordParts[0].trim());
                                    latitude = Double.parseDouble(coordParts[1].trim());
                                } catch (NumberFormatException e) {
                                    // Ignorer si ce n'est pas un nombre valide
                                }
                            }
                        }
                        
                        // Si on a trouvé des coordonnées valides et un code IATA, on ajoute l'aéroport
                        if (!codeIATA.isEmpty() && latitude != 0.0 && longitude != 0.0) {
                            list.add(new Aeroport(nom, latitude, longitude, codeIATA));
                        }
                    } catch (Exception e) {
                        // Ignorer les lignes mal formées
                    }
                }
                s = buf.readLine();
            }
            buf.close();
        } catch (Exception e) {
            System.out.println("Maybe the file isn't there ?");
            if (!list.isEmpty()) {
                System.out.println(list.get(list.size() - 1));
            }
            e.printStackTrace();
        }
    }

    /**
     * Retourne la liste des aéroports
     * @return La liste des aéroports
     */
    public List<Aeroport> getList() {
        return list;
    }

    /**
     * Trouve l'aéroport le plus proche d'un point donné par ses coordonnées GPS
     * @param longitude La longitude du point
     * @param latitude La latitude du point
     * @return L'aéroport le plus proche
     */
    public Aeroport findNearestAirport(double longitude, double latitude) {
        if (list.isEmpty()) {
            return null;
        }
        
        Aeroport nearest = list.get(0);
        double minDistance = distance(longitude, latitude, nearest.getLongitude(), nearest.getLatitude());
        
        for (Aeroport a : list) {
            double dist = distance(longitude, latitude, a.getLongitude(), a.getLatitude());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = a;
            }
        }
        
        return nearest;
    }

    /**
     * Trouve un aéroport par son code IATA
     * @param code Le code IATA recherché
     * @return L'aéroport correspondant, ou null si non trouvé
     */
    public Aeroport findByCode(String code) {
        for (Aeroport a : list) {
            if (a.getCodeIATA().equals(code)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Calcule la distance entre deux points GPS
     * Utilise la formule simplifiée : norme = (lat2-lat1)² + (lon2-lon1)² * cos²((lat2+lat1)/2)
     * @param lon1 Longitude du premier point
     * @param lat1 Latitude du premier point
     * @param lon2 Longitude du deuxième point
     * @param lat2 Latitude du deuxième point
     * @return La norme de la distance
     */
    public double distance(double lon1, double lat1, double lon2, double lat2) {
        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;
        double moyenneLat = (lat2 + lat1) / 2.0;
        
        return deltaLat * deltaLat + deltaLon * deltaLon * Math.cos(Math.toRadians(moyenneLat)) * Math.cos(Math.toRadians(moyenneLat));
    }

    /**
     * Méthode main pour tester la classe World
     */
    public static void main(String[] args) {
        World w = new World("./data/airport-codes_no_comma.csv");
        System.out.println("Found " + w.getList().size() + " airports.");
        Aeroport paris = w.findNearestAirport(2.316, 48.866);
        Aeroport cdg = w.findByCode("CDG");
        double distance = w.distance(2.316, 48.866, paris.getLongitude(), paris.getLatitude());
        System.out.println(paris);
        System.out.println(distance);
        double distanceCDG = w.distance(2.316, 48.866, cdg.getLongitude(), cdg.getLatitude());
        System.out.println(cdg);
        System.out.println(distanceCDG);
    }
}


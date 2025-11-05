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
     * Constructeur qui lit le fichier CSV et crée la liste des aéroports
     * @param fileName Le chemin vers le fichier CSV contenant les aéroports
     */
    public World(String fileName) {
        list = new ArrayList<>();
        try {
            BufferedReader buf = new BufferedReader(new FileReader(fileName));
            String s = buf.readLine();
            while (s != null) {
                s = s.replaceAll("\"", ""); // Enlève les guillemets qui séparent les champs de données GPS
                String fields[] = s.split(",");
                
                // On ne garde que les "large_airport"
                if (fields.length > 1 && fields[1].equals("large_airport")) {
                    try {
                        // Le format du CSV est : code,type,nom,altitude,continent,country,region,ville,code_ICAO,code_IATA,...
                        // Les coordonnées GPS sont dans un champ avec format "longitude, latitude"
                        String nom = fields.length > 2 ? fields[2] : "";
                        String codeIATA = "";
                        double latitude = 0.0;
                        double longitude = 0.0;
                        
                        // Chercher le code IATA (généralement dans le champ 9, index 9)
                        if (fields.length > 9 && !fields[9].isEmpty()) {
                            codeIATA = fields[9];
                        }
                        
                        // Chercher les coordonnées GPS dans la ligne originale
                        // Format typique : "longitude, latitude" dans un champ
                        // On cherche un pattern de deux nombres séparés par une virgule et un espace
                        java.util.regex.Pattern coordPattern = java.util.regex.Pattern.compile("(-?\\d+\\.?\\d*),\\s*(-?\\d+\\.?\\d*)");
                        java.util.regex.Matcher matcher = coordPattern.matcher(s);
                        if (matcher.find()) {
                            try {
                                longitude = Double.parseDouble(matcher.group(1).trim());
                                latitude = Double.parseDouble(matcher.group(2).trim());
                            } catch (NumberFormatException e) {
                                // Ignorer si ce n'est pas un nombre valide
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


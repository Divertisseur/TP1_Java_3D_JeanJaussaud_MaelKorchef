/**
 * Classe Aeroport représentant un aéroport avec son nom, ses coordonnées GPS et son code IATA
 */
public class Aeroport {
    private String nom;
    private double latitude;
    private double longitude;
    private String codeIATA;

    /**
     * Constructeur de la classe Aeroport
     * @param nom Le nom de l'aéroport
     * @param latitude La latitude de l'aéroport
     * @param longitude La longitude de l'aéroport
     * @param codeIATA Le code IATA de l'aéroport
     */
    public Aeroport(String nom, double latitude, double longitude, String codeIATA) {
        this.nom = nom;
        this.latitude = latitude;
        this.longitude = longitude;
        this.codeIATA = codeIATA;
    }

    // Getters
    public String getNom() {
        return nom;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCodeIATA() {
        return codeIATA;
    }

    /**
     * Calcule la distance entre cet aéroport et un autre aéroport
     * Utilise la formule simplifiée : norme = (lat2-lat1)² + (lon2-lon1)² * cos²((lat2+lat1)/2)
     * @param a L'aéroport avec lequel calculer la distance
     * @return La norme de la distance (non la vraie distance)
     */
    public double calculDistance(Aeroport a) {
        double deltaLat = a.getLatitude() - this.latitude;
        double deltaLon = a.getLongitude() - this.longitude;
        double moyenneLat = (a.getLatitude() + this.latitude) / 2.0;
        
        return deltaLat * deltaLat + deltaLon * deltaLon * Math.cos(Math.toRadians(moyenneLat)) * Math.cos(Math.toRadians(moyenneLat));
    }

    @Override
    public String toString() {
        return "Aeroport{" +
                "nom='" + nom + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", codeIATA='" + codeIATA + '\'' +
                '}';
    }
}


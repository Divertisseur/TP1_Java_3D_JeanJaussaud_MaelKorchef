/**
 * Classe Flight modélisant un vol
 * Un vol est défini par deux aéroports : départ et arrivée
 */
public class Flight {
    private Aeroport depart;
    private Aeroport arrivee;

    /**
     * Constructeur de la classe Flight
     * @param depart L'aéroport de départ
     * @param arrivee L'aéroport d'arrivée
     */
    public Flight(Aeroport depart, Aeroport arrivee) {
        this.depart = depart;
        this.arrivee = arrivee;
    }

    // Getters
    public Aeroport getDepart() {
        return depart;
    }

    public Aeroport getArrivee() {
        return arrivee;
    }

    @Override
    public String toString() {
        return "Flight{" +
                "depart=" + (depart != null ? depart.getCodeIATA() : "null") +
                ", arrivee=" + (arrivee != null ? arrivee.getCodeIATA() : "null") +
                '}';
    }
}


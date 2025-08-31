package domain;

import java.sql.Timestamp;

/**
 * Represents an exchange rate record from the exchange_points table
 */
public class ExchangeRateRecord {
    private int id;
    private int pointsPerEGP;
    private double egpPerPoints;
    private Timestamp lastUpdated;

    public ExchangeRateRecord(int id, int pointsPerEGP, double egpPerPoints, Timestamp lastUpdated) {
        this.id = id;
        this.pointsPerEGP = pointsPerEGP;
        this.egpPerPoints = egpPerPoints;
        this.lastUpdated = lastUpdated;
    }

    // Getters
    public int getId() { return id; }
    public int getPointsPerEGP() { return pointsPerEGP; }
    public double getEgpPerPoints() { return egpPerPoints; }
    public Timestamp getLastUpdated() { return lastUpdated; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setPointsPerEGP(int pointsPerEGP) { this.pointsPerEGP = pointsPerEGP; }
    public void setEgpPerPoints(double egpPerPoints) { this.egpPerPoints = egpPerPoints; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    public String toString() {
        return "Rate #" + id + ": " + pointsPerEGP + " points = 1 EGP (" + 
               String.format("%.4f", egpPerPoints) + " EGP per point) - " + lastUpdated;
    }
}

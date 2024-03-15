package org.acme;
import java.util.logging.Logger;

public class Voiture {
    Logger logger = Logger.getLogger(getClass().getName());
    private int x;
    private int y;
    private int direction;
    private int carburant = 60;

    public Voiture() {
        this.x = 0;
        this.y = 0;
        this.direction = 0; // Face au nord par défaut
    }

    public void avancer(int cases) {
        if (carburant >= (cases / 3)) {
            switch (direction) {
                case 0: y += cases; break; // Nord
                case 1: x += cases; break; // Est
                case 2: y -= cases; break; // Sud
                case 3: x -= cases; break; // Ouest
                default: break;
            }
            carburant -= (cases / 3);
        } else {
            logger.info("Pas assez de carburant pour se déplacer!");
        }
    }

    public void pivoterDroite() {
        direction = (direction + 1) % 4;
    }

    public void pivoterGauche() {
        direction = (direction + 3) % 4;
    }

    public void recharger() {
        carburant = 60;
    }

    public void afficherPosition() {
        logger.info("Position: (" + x + ", " + y + "), Direction: " + direction + ", Carburant: " + carburant);
    }
}

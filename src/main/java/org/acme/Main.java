package org.acme;

public class Main {
    public static class Jeux {
        public String nom;

        public Jeux(String nom) {
            this.nom = nom;
        }
    }

    public static String title(Jeux jeu, int p) {
        String nom = jeu.nom;
        String res = "";
        for (int i = 0; i <= p && i < 100 - jeu.nom.length(); i++) {
            res += "#";
        }
        res += nom;
        return res;
    }

    public static void animateTitle(Jeux jeu) {
        try {
            for (int p = 0; p < 100 - jeu.nom.length(); p++) {
                System.out.print("\r" + title(jeu, p));
                System.out.flush();
                Thread.sleep(200); // Délai de 100 ms entre chaque mise à jour
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Jeux jeu = new Jeux("Voitures");
        System.out.println(title(jeu, 10));
        animateTitle(jeu);
    }
}

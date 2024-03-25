package org.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.fasterxml.jackson.databind.ObjectMapper; // Assurez-vous d'inclure la dépendance Jackson dans votre projet

@ServerEndpoint("/ws/game")
public class GameWebSocket {

    static final Map<String, Position> positions = new ConcurrentHashMap<>();
    private static final Set<Square> squares = new CopyOnWriteArraySet<>(); // Pour stocker les carrés

    private static final ObjectMapper objectMapper = new ObjectMapper(); // Pour convertir les objets en JSON
    static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Random random = new Random();
    private static Timer timer = new Timer();
    private static boolean isTaskScheduled = false;



    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected: " + session.getId());
        sessions.add(session);
        positions.put(session.getId(), new Position(random.nextInt(1200), random.nextInt(700), session.getId()));
        broadcastState();

        // Planifier la génération de carrés jaunes uniquement si elle n'a pas encore été planifiée
        synchronized (GameWebSocket.class) {
            if (!isTaskScheduled) {
                // Initialisation des carrés
                generateAndBroadcastSquares();

                // Déplacement des carrés toutes les secondes
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        moveSquares();
                    }
                }, 1000, 1000);

                isTaskScheduled = true;
            }
        }

    }


    @OnMessage
    public void onMessage(String message, Session session) throws JsonProcessingException {
        // Supposition : message est un JSON avec les états des touches, par exemple {"up":true,"right":true}
        Map<String, Boolean> keyStates = objectMapper.readValue(message, new TypeReference<Map<String, Boolean>>(){});

        Position currentPosition = positions.get(session.getId());

        // Déterminez la direction basée sur les états des touches
        int dx = 0, dy = 0;
        //max entre (8 - 0.1*currentPosition.getScore()) et 2
        if (Boolean.TRUE.equals(keyStates.get("up"))) dy -= (int) Math.max(2, 8 - 0.1*currentPosition.getScore());
        if (Boolean.TRUE.equals(keyStates.get("down"))) dy += (int) Math.max(2, 8 - 0.1*currentPosition.getScore());
        if (Boolean.TRUE.equals(keyStates.get("left"))) dx -= (int) Math.max(2, 8 - 0.1*currentPosition.getScore());
        if (Boolean.TRUE.equals(keyStates.get("right"))) dx += (int) Math.max(2, 8 - 0.1*currentPosition.getScore());

        // Appliquez le mouvement
        currentPosition.setX(currentPosition.getX() + dx);
        currentPosition.setY(currentPosition.getY() + dy);

        // Assurez-vous d'effectuer tout traitement nécessaire, comme la vérification des collisions
        checkAndHandleCollisions();

        // Diffusez l'état actualisé à tous les clients
        broadcastState();
    }


    @OnClose
    public void onClose(Session session) {
        System.out.println("Disconnected: " + session.getId());
        sessions.remove(session);
        positions.remove(session.getId());
        broadcastState();
    }

    // Diffuse l'état actuel du jeu à tous les joueurs connectés
    private void broadcastState() {
        checkAndHandleCollisions();
        String stateJson = "";
        try {
            stateJson = objectMapper.writeValueAsString(positions);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        for (Session s : sessions) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendText(stateJson);
                System.out.println("Sent to: " + s.getId() + " -> " + stateJson);
            }
        }
    }

    private void generateAndBroadcastSquares() {
        // Générer des positions aléatoires pour les carrés
        squares.clear(); // Supprimer les anciens carrés
        for (int i = 0; i < 10; i++) { // Générer 5 carrés par exemple
            int x = random.nextInt(1200); // Supposons que votre canvas fait 800x600
            int y = random.nextInt(700);
            squares.add(new Square(x, y));
        }

        broadcastSquares();
    }

    private Square generateNewSquare() {
        int x = random.nextInt(1200); // Assurez-vous que ces valeurs correspondent à la taille de votre zone de jeu
        int y = random.nextInt(700);
        return new Square(x, y);
    }


    private void moveSquares() {
        squares.forEach(square -> {
            // Choisissez une direction aléatoire: 0=gauche, 1=droit, 2=haut, 3=bas
            int direction = random.nextInt(4);
            switch (direction) {
                case 0: square.setX(Math.max(0, square.getX() - 10)); break; // Gauche
                case 1: square.setX(Math.min(790, square.getX() + 10)); break; // Droite
                case 2: square.setY(Math.max(0, square.getY() - 10)); break; // Haut
                case 3: square.setY(Math.min(590, square.getY() + 10)); break; // Bas
            }
        });
        broadcastSquares();
    }


    private void broadcastSquares() {
        String squaresJson = "";
        try {
            squaresJson = objectMapper.writeValueAsString(new ArrayList<>(squares)); // Convertir en liste pour assurer la sérialisation correcte
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (Session session : sessions) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(squaresJson);
            }
        }
    }

    private void checkAndHandleCollisions() {
        List<Square> eatenSquares = new ArrayList<>();
        positions.values().forEach(position -> squares.forEach(square -> {
            if (Math.abs(position.getX() - square.getX()) < 20+ 2*position.getScore() && Math.abs(position.getY() - square.getY()) < 20 + 2*position.getScore()) {
                eatenSquares.add(square);
                position.incrementScore(); // Supposons que vous ayez ajouté une méthode pour gérer le score
                // Ajouter un nouveau carré à chaque fois qu'un est mangé
                squares.add(generateNewSquare());
            }
        }));
        squares.removeAll(eatenSquares); // Retirer les carrés mangés

        // Nouveau code pour gérer les collisions entre joueurs
        List<String> sessionsToBeRemoved = new ArrayList<>();
        positions.values().forEach(position1 -> positions.values().forEach(position2 -> {
            if (!position1.getId().equals(position2.getId()) &&
                    Math.sqrt(Math.pow(position1.getX() - position2.getX(), 2) + Math.pow(position1.getY() - position2.getY(), 2)) < 40 + 2 * Math.max(position1.getScore(), position2.getScore())) {
                // Collision détectée, comparez les scores
                if (position1.getScore() > position2.getScore()) {
                    sessionsToBeRemoved.add(position2.getId());
                    position1.incrementScore(position2.getScore()); // Incrémentez du score de l'autre joueur
                } else if (position2.getScore() > position1.getScore()) {
                    sessionsToBeRemoved.add(position1.getId());
                    position2.incrementScore(position1.getScore()); // Incrémentez du score de l'autre joueur
                }
            }
        }));

        // Gérer la déconnexion et la suppression des sessions
        sessionsToBeRemoved.forEach(sessionId -> {
            Session session = sessions.stream().filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
            if (session != null) {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Vous avez été déconnecté en raison d'une collision"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sessions.remove(session);
                positions.remove(sessionId);
            }
        });

        // N'oubliez pas d'appeler broadcastState() si nécessaire pour mettre à jour tous les clients
        if (!sessionsToBeRemoved.isEmpty()) {
            broadcastState();
        }
    }






    // Classe pour représenter la position d'un joueur
    public static class Position {
        private int x, y;
        private String id; // ID de la session
        private String pseudo; // Pseudo du joueur
        private String color; // Couleur du joueur
        private int score = 0; // Score initial du joueur

        public Position(int x, int y, String id) {
            this.x = x;
            this.y = y;
            this.id = id;
            this.pseudo = generatePseudo(); // Générer un pseudo aléatoire
            this.color = generateColor(); // Générer une couleur aléatoire
        }

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public String getId() { return id; } // Getter pour l'ID
        public String getPseudo() { return pseudo; } // Getter pour le pseudo
        public String getColor() { return color; } // Getter pour la couleur

        public int getScore() {
            return score;
        }

        public void incrementScore() {
            this.score++;
        }

        public void incrementScore(int score) {
            this.score += score;
        }

        private String generatePseudo() {
            Random random = new Random();
            int pseudo = 100 + random.nextInt(900); // Génère un nombre entre 100 et 999
            return String.valueOf(pseudo);
        }

        private String generateColor() {
            Random rand = new Random();
            // Génère une couleur hexadécimale
            int r = rand.nextInt(256);
            int g = rand.nextInt(256);
            int b = rand.nextInt(256);
            return String.format("#%02x%02x%02x", r, g, b);
        }
    }

    class Square {
        private int x, y;

        public Square(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
    }
}
# Utiliser une image de base Java
FROM openjdk:11

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers de build Gradle et les sources
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Donner les droits d'exécution au script gradlew
RUN chmod +x ./gradlew

# Construire l'application avec Gradle
RUN ./gradlew build

# Exposer le port utilisé par l'application
EXPOSE 8080

# Lancer l'application Java
CMD ["java", "-jar", "build/quarkus-app/quarkus-run.jar"]

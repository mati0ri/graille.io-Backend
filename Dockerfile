# Utiliser une image de base Java
FROM openjdk:11

# Définir le répertoire de travail dans le conteneur
WORKDIR /app

# Copier le wrapper gradle et les scripts
COPY gradlew .
COPY gradle gradle

# Donner les droits d'exécution au script gradlew
RUN chmod +x ./gradlew

# Copier les fichiers de configuration Gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Copier le répertoire src
COPY src src

# Exécuter la construction de l'application avec Gradle
# Utilisez l'option --no-daemon pour éviter les processus en arrière-plan inutiles dans le conteneur
RUN ./gradlew build --no-daemon

# Exposer le port utilisé par l'application
EXPOSE 8080

# Lancer l'application Quarkus
# Assurez-vous que le chemin du JAR est correct selon la sortie de la construction Gradle
CMD ["java", "-jar", "build/quarkus-app/quarkus-run.jar"]

FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy dependency JARs
COPY lib/*.jar lib/

# Copy all source code (including backend and frontend src)
COPY src/ ./src/

# Compile all Java sources
RUN mkdir -p build/classes && \
    find src -name "*.java" > sources.txt && \
    javac -d build/classes -cp "lib/*" @sources.txt

CMD ["java", "-cp", "build/classes:lib/*", "backend.Main"]

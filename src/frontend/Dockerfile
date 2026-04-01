FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy source and lib
COPY src/ src/
COPY lib/ lib/

# Compile all backend + frontend sources
RUN mkdir -p build/classes && \
    find src -name "*.java" > sources.txt && \
    javac -d build/classes -cp "lib/*" @sources.txt

ENTRYPOINT ["java", "-cp", "build/classes:lib/*", "frontend.BookingUI"]


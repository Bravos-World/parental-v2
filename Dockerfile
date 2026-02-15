# Build stage
FROM eclipse-temurin:25.0.2_10-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle/ gradle/

RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar

RUN jdeps --ignore-missing-deps -q \
    --recursive \
    --multi-release 25 \
    --print-module-deps \
    --class-path 'app/build/libs/*' \
    build/libs/parentalv2.jar > deps.txt

RUN jlink \
    --add-modules $(cat deps.txt),java.base,java.logging,java.naming,jdk.compiler,java.desktop,java.management,java.security.jgss,java.instrument,jdk.crypto.ec,jdk.unsupported \
    --compress zip-9 \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --output /custom-jre

# Runtime stage
FROM gcr.io/distroless/base-debian12

WORKDIR /app

COPY --from=build /custom-jre /opt/java/openjdk
COPY --from=build /app/build/libs/parentalv2.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["/opt/java/openjdk/bin/java", "-jar", "app.jar"]
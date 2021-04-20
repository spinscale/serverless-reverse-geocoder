FROM ghcr.io/graalvm/graalvm-ce:java11-21.0.0.2 AS builder
RUN gu install native-image
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew :webserver:nativeImage

RUN ls -al /home/gradle/src/webserver/build/bin/

FROM busybox
WORKDIR /app
# copy binary
COPY --from=builder /home/gradle/src/webserver/build/bin/webserver /app/webserver
RUN ls -al /app
# FST & lucene directory
ADD webserver/build/data /app/data
ADD webserver/src/test/resources/auth.fst /app/auth.fst
ENTRYPOINT [ "/app/webserver" ]
# the PORT has been omitted here, as this is set by google cloud run
ENV AUTH_FILE="/app/auth.fst" INDEX_DIRECTORY="/app/data/"

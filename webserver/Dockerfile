FROM oracle/graalvm-ce:20.3.0-java11 AS builder
RUN gu install native-image
ADD build/libs/webserver-all-0.0.1.jar /webserver.jar
RUN native-image -jar /webserver.jar -da -H:+ReportExceptionStackTraces -H:-UseServiceLoaderFeature --report-unsupported-elements-at-runtime --no-fallback --static --initialize-at-build-time /webserver


FROM busybox
WORKDIR /app
# copy binary
COPY --from=builder /webserver /app/webserver
# FST & lucene directory
ADD build/data /app/data
ADD src/test/resources/auth.fst /app/auth.fst
ENTRYPOINT [ "/app/webserver" ]
# the PORT has been omitted here, as this is set by google cloud run
ENV AUTH_FILE="/app/auth.fst" INDEX_DIRECTORY="/app/data/"

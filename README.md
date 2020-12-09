# Running A Serverless Lucene Reverse Geocoder

This is the accompanying GitHub repository for a long blog post at
https://spinscale.de/posts/2020-12-09-running-serverless-lucene-reverse-geocoder.html

You can read the motivation behind this repository at the above blog post. You
will learn:

* How a reverse geocoder works
* Finding public data sets to implement your reverse geocoder
* Index geo shapes & points with Apache Lucene on Java 11 & Gradle
* Write a CLI application querying Apache Lucene and speed it up using GraalVM
* Deploy the app & index on AWS Lambda using the serverless framework
* Create a small web application using Javalin with built-in authorization
* Deploy a compiled binary on Google Cloud Run enjoying millisecond startups

This README will show you only the most needed commands. **Note**: This has
only been tested with osx and will not work under windows at all, as I have not
spent a lot of time fixing all the paths.

Make sure GraalVM is installed, if you have [sdkman](https://sdkman.io) installed, run

```bash
sdk install java 20.3.0.r11-grl
```

Download the geo shape and point data by running 

```bash
./gradlew :indexer:downloadShapes :indexer:downloadCsvPointFile
```

Initial index creation is part of the assembly, as well as creating uber jars

```bash
./gradlew clean check assemble
```

You can run the uber jar CLI application now like

```bash
java -jar cli/build/libs/cli-all-0.0.1.jar indexer/build/indices 48.13 11.57
```

Native images are not built by default, but you can run

```bash
./gradlew :cli:nativeImage
```

You can run the CLI application now like

```bash
./cli/build/bin/cli-searcher indexer/build/indices 48.1374 11.5755
```

Creating the docker image can be done via (this will take some time as building
in Docker might not be super fast)

```bash
./gradlew :webserver:dockerBuildImage
```

You can run that created docker image locally via 

```bash
./gradlew :webserver:dockerRunLatest
```

You should see a rather fast start up like

```
[main] INFO io.javalin.Javalin - Listening on http://localhost:7000/
[main] INFO io.javalin.Javalin - Javalin started in 4ms \o/
```

Now you can call one of the following curl call to test if everything has
worked and see different response codes in action

```bash
# successful search
curl -X POST http://localhost:7000/search \
  --header "Authorization: allowed-token" \
  -d '{"latitude":48.14, "longitude":11.57}'

# error, as token is rejected
curl -X POST http://localhost:7000/search \
  --header "Authorization: rejected-token" \
  -d '{"latitude":48.14, "longitude":11.57}'


# successful health
curl http://localhost:7000/health \
  --header "Authorization: operations-token"

# returning 403
curl http://localhost:7000/health
```

If you want to deploy to AWS Lambda or Google Cloud Run, please check out the 
[blog post](https://spinscale.de/posts/2020-12-09-running-serverless-lucene-reverse-geocoder.html).

Feel free to fork or submit fixes and improvements to this!

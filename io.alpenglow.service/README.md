# io.alpenglow.webapp

Minimal Helidon SE project suitable to start from scratch.

## Build and run


With JDK17+
```bash
mvn package
java -jar target/io.alpenglow.webapp.jar
```

## Exercise the application
```
curl -X GET http://localhost:8080/simple-greet
{"message":"Hello World!"}
```



## Building a Native Image

Make sure you have GraalVM locally installed:

```
$GRAALVM_HOME/bin/native-image --version
```

Build the native image using the native image profile:

```
mvn package -Pnative-image
```

This uses the helidon-maven-plugin to perform the native compilation using your installed copy of GraalVM. It might take a while to complete.
Once it completes start the application using the native executable (no JVM!):

```
./target/io.alpenglow.webapp
```

Yep, it starts fast. You can exercise the application’s endpoints as before.


## Building the Docker Image
```
docker build -t io.alpenglow.webapp .
```

## Running the Docker Image

```
docker run --rm -p 8080:8080 io.alpenglow.webapp:latest
```

Exercise the application as described above.
                                


# 1st stage, build the app
FROM debian:stretch-slim

# RUN apk update && apk add bash

WORKDIR /quickstart
COPY /target/io.alpenglow.quickstart-jri ./

ENTRYPOINT ["/quickstart/bin/start"]

EXPOSE 8080

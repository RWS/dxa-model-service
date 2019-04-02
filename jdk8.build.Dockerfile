FROM maven:3.6-jdk-8-alpine
RUN apk add --update git && rm -rf /var/cache/apk/*

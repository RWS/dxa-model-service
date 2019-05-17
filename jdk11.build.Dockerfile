FROM maven:3.6-jdk-11-slim
RUN apt-get update && apt-get -y install git && apt-get remove

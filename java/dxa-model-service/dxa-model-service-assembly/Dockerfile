FROM 176207018055.dkr.ecr.eu-west-1.amazonaws.com/content-delivery/java8-awscli:10.0.0-1020

RUN apt-get update && apt-get clean all && rm -rf /var/lib/apt/lists/*

RUN mkdir /dxa-model-service && chmod 755 /dxa-model-service
COPY target/dxa-model-service/standalone /dxa-model-service
WORKDIR /dxa-model-service

RUN chmod +x bin/start.sh

CMD /bin/bash -C 'bin/start.sh' "--server.port=8998 --logging.config=config/logback.xml $startparameters"

EXPOSE 8998
EXPOSE 8082
MAINTAINER SDL

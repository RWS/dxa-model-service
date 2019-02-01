FROM 176207018055.dkr.ecr.eu-west-1.amazonaws.com/content-delivery/java8-awscli:10.0.0-1020

RUN apt-get update && apt-get clean all && rm -rf /var/lib/apt/lists/*

RUN mkdir /dxa-model-service && chmod 755 /dxa-model-service
COPY target/dxa-model-service/standalone /dxa-model-service
WORKDIR /dxa-model-service

RUN chmod +x bin/start.sh
RUN chmod +x bin/stop.sh

RUN sed -i 's/& echo \$! > \$PID_FILE//g' bin/start.sh
RUN sed -i 's/The service is not started\./Container stopped./g' bin/stop.sh

WORKDIR /dxa-model-service/bin

CMD echo "Waiting for discovery-service to be up and running" && \
    while [ "$(curl --write-out %{http_code} --silent --output /dev/null ${tokenurl})" -ne "200" ]; do sleep 1; done && \
    echo "discovery-service is up and running" && /bin/bash -C 'cloud-start.sh' "$startparameters"

EXPOSE 8998
EXPOSE 8082

MAINTAINER SDL

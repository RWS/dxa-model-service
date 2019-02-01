FROM 176207018055.dkr.ecr.eu-west-1.amazonaws.com/content-delivery/java8-awscli:11.0.0-1024

RUN apt-get update && apt-get clean all && rm -rf /var/lib/apt/lists/*

RUN mkdir /dxa-model-service && chmod 755 /dxa-model-service
COPY target/dxa-model-service/standalone /dxa-model-service
COPY target/dxa-model-service/standalone/cloud-assembly/bin /dxa-model-service/bin

WORKDIR /dxa-model-service

RUN chmod +x bin/*.sh

RUN sed -i 's/& echo \$! > \$PID_FILE//g' bin/start.sh
# RUN sed -i 's/${AWS_SSM_PREFIX}.${PARAM}/${AWS_SSM_PREFIX}${PARAM}/g' bin/start.sh
#RUN sed -i 's/eval export $PARAM=\\\$value/\
#            if [ "$value" == "None" ]; then \n echo "Skipping not set SSM param ${AWS_SSM_PREFIX}${PARAM}" \n \
#            else \n echo "Exporting SSM param ${PARAM}" \n eval export $PARAM=\\\$value \n fi/g' \
#            bin/start.sh
RUN sed -i 's/The service is not started\./Container stopped./g' bin/stop.sh

RUN chmod +x installer/prepare.sh

WORKDIR /dxa-model-service/installer
RUN ./prepare.sh loadDependencies

CMD /bin/bash -C '/dxa-model-service/bin/ssmconfigure.sh' && \
   echo "Waiting for discovery-service to be up and running" && \
   while [ "$(curl --write-out %{http_code} --silent --output /dev/null ${TOKEN_URL})" -ne "200" ]; do sleep 1; done && \
   echo "discovery-service is up and running" && \
   /bin/bash -C prepare.sh --console=plain -PrunScript=run -Pbatch

EXPOSE 8998
EXPOSE 8082

MAINTAINER SDL

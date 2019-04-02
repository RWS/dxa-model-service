#!/usr/bin/env bash

# First we rebuild the project
mvn clean install -f ..

# Then we login to AWS
eval $( aws ecr get-login --no-include-email --region eu-west-1 ) 

# Then we build the image and push it
docker build -t 176207018055.dkr.ecr.eu-west-1.amazonaws.com/dxa/model-service . 
docker push 176207018055.dkr.ecr.eu-west-1.amazonaws.com/dxa/model-service:latest

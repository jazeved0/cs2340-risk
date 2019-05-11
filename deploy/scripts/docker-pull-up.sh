#!/bin/bash

sudo docker stop risk-main
sudo docker rm risk-main
sudo docker pull jazevedo6/cs2340-risk:latest
sudo docker run -d \
    --name risk-main \
    -e 'LETSENCRYPT_EMAIL=joseph.az@gatech.edu' \
    -e 'LETSENCRYPT_HOST=riskgame.ga' \
    -e 'VIRTUAL_HOST=riskgame.ga' jazevedo6/cs2340-risk:latest

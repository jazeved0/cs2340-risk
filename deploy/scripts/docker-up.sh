#!/bin/bash

cd
sudo docker pull jazevedo6/risk-main:latest
sudo docker run -d -p 80:80 -p 443:443 \
    --name nginx-proxy \
    -v $HOME/certs:/etc/nginx/certs:ro \
    -v /etc/nginx/vhost.d \
    -v /usr/share/nginx/html \
    -v /var/run/docker.sock:/tmp/docker.sock:ro \
    -e 'HTTPS_METHOD=redirect' \
    --label com.github.jrcs.letsencrypt_nginx_proxy_companion.nginx_proxy=true \
    jwilder/nginx-proxy:alpine
sudo docker run -d \
    --name nginx-letsencrypt \
    --volumes-from nginx-proxy \
    -v $HOME/certs:/etc/nginx/certs:rw \
    -v /var/run/docker.sock:/var/run/docker.sock:ro \
    jrcs/letsencrypt-nginx-proxy-companion
sudo docker run -d \
    --name risk-main \
    -e 'LETSENCRYPT_EMAIL=joseph.az@gatech.edu' \
    -e 'LETSENCRYPT_HOST=riskgame.ga' \
    -e 'VIRTUAL_HOST=riskgame.ga' jazevedo6/cs2340-risk:latest

FROM openjdk:8-jre-alpine
COPY svc /svc
EXPOSE 9000 9443
RUN apk add --no-cache bash
CMD /svc/bootstrap

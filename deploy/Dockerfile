FROM openjdk:8-jre-alpine
COPY svc /svc
EXPOSE 80
RUN apk add --no-cache bash
CMD /svc/bootstrap

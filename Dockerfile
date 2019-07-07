FROM adoptopenjdk/openjdk11@sha256:eaa182283f19d3f0ee0c6217d29e299bb4056d379244ce957e30dcdc9e278e1e

RUN ["apk", "--no-cache", "upgrade"]

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

# Add RDS CA certificates to the default truststore
RUN wget -qO - https://s3.amazonaws.com/rds-downloads/rds-ca-2015-root.pem       | keytool -importcert -noprompt -cacerts -storepass changeit -alias rds-ca-2015-root \
 && wget -qO - https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem | keytool -importcert -noprompt -cacerts -storepass changeit -alias rds-combined-ca-bundle

RUN ["apk", "add", "--no-cache", "bash", "tini"]

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

ADD docker-startup.sh /app/docker-startup.sh
ADD target/*.yaml /app/
ADD target/pay-*-allinone.jar /app/

RUN java -Xshare:off -XX:DumpLoadedClassList=cds.lst -jar pay-*-allinone.jar -v \
 && java -Xshare:dump -XX:SharedClassListFile=cds.lst -XX:SharedArchiveFile=cds.jsa -jar pay-*-allinone.jar \
 && java -Xshare:on -XX:SharedClassListFile=cds.lst -XX:SharedArchiveFile=cds.jsa -jar pay-*-allinone.jar -v

ENTRYPOINT ["tini", "-e", "143", "--"]

CMD ["bash", "./docker-startup.sh"]

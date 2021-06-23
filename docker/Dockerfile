# syntax=docker/dockerfile:experimental

FROM maven:3.6-jdk-8 as build
WORKDIR /dremio
COPY . ./dremio-oss/
WORKDIR /dremio/dremio-oss
RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/dremio:cache-alpine mvn -T 1C install -DskipTests=true -Dlicense.skip -pl :dremio-common -am
RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/dremio:cache-alpine mvn -T 1C package -DskipTests=true -Dlicense.skip -pl :dremio-services-users
RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/dremio:cache-alpine mvn -T 1C package -DskipTests=true -Dlicense.skip -pl :dremio-sabot-logical
RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/dremio:cache-alpine mvn -T 1C package -Dmaven.test.skip=true -Dlicense.skip -pl :dremio-dac-backend
RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/dremio:cache-alpine mvn -T 1C package -DskipTests=true -Dlicense.skip -pl :dremio-dac-ui
RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/dremio:cache-alpine mvn -T 1C package -Dmaven.test.skip=true -Dlicense.skip -pl :dremio-distribution -am
RUN mkdir -p /opt/dremio && mv /dremio/dremio-oss/distribution/server/target/dremio*.tar.gz /opt/dremio/dremio.tar.gz
RUN tar vxfz /opt/dremio/dremio.tar.gz -C /opt/dremio --strip-components=1 && rm /opt/dremio/dremio.tar.gz

FROM adoptopenjdk/openjdk8:alpine as run
LABEL org.label-schema.name='dremio/dremio-oss'
LABEL org.label-schema.description='Dremio OSS.'

# set user configurations
ARG USER=dremio
ARG USER_ID=1025
ARG USER_GROUP=dremio
ARG USER_GROUP_ID=1025
ARG USER_HOME=/home/${USER}

RUN  addgroup -g ${USER_GROUP_ID} ${USER_GROUP}; \
     adduser -u ${USER_ID} -D -g '' -h ${USER_HOME} -G ${USER_GROUP} ${USER} ;

RUN \
  mkdir -p /opt/dremio \
  && mkdir -p /var/lib/dremio \
  && mkdir -p /var/run/dremio \
  && mkdir -p /var/log/dremio \
  && mkdir -p /opt/dremio/data \
  && chown -R dremio:dremio /opt/dremio/data \
  && chown -R dremio:dremio /var/run/dremio \
  && chown -R dremio:dremio /var/log/dremio \
  && chown -R dremio:dremio /var/lib/dremio

COPY --chown=dremio:dremio --from=build /opt/dremio /opt/dremio

EXPOSE 9047/tcp
EXPOSE 31010/tcp
EXPOSE 45678/tcp

USER dremio
WORKDIR /opt/dremio
ENV DREMIO_HOME /opt/dremio
ENV DREMIO_PID_DIR /var/run/dremio
ENV DREMIO_GC_LOGS_ENABLED="no"
ENV DREMIO_LOG_DIR="/var/log/dremio"
ENV SERVER_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps"
ENTRYPOINT ["bin/dremio", "start-fg"]

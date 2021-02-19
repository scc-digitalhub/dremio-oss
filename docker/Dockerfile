#
# Copyright (C) 2017 Dremio Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# #baseDEP all offline, >1.2GB 1h
# FROM maven:3-jdk-8 as basedep
# WORKDIR /dremio
# RUN git clone -b multitenancy https://github.com/smartcommunitylab/dremio-oss.git
# WORKDIR /dremio/dremio-oss
# RUN mvn -Dmaven.repo.local=/dremio/m2 -Dmaven.artifact.threads=100 dependency:go-offline

#baseDEP min cache 20M <1m
FROM maven:3-jdk-8 as basedep
WORKDIR /dremio
RUN git clone -b multitenancy https://github.com/smartcommunitylab/dremio-oss.git
WORKDIR /dremio/dremio-oss
RUN mvn -Dmaven.repo.local=/dremio/m2 -Dmaven.artifact.threads=100 clean

FROM maven:3-jdk-8 as basebuild
WORKDIR /dremio
COPY --from=basedep /dremio/m2/ ./m2/
RUN git clone -b multitenancy https://github.com/smartcommunitylab/dremio-oss.git
WORKDIR /dremio/dremio-oss
RUN mvn -Dmaven.repo.local=/dremio/m2 -T 1C install -DskipTests=true -Dlicense.skip -pl :dremio-common -am
RUN mvn -Dmaven.repo.local=/dremio/m2 -T 1C package -DskipTests=true -Dlicense.skip -pl :dremio-sabot-logical

FROM maven:3-jdk-8 as build
WORKDIR /dremio
COPY --from=basebuild /dremio/m2/ ./m2/
COPY --from=basebuild /dremio/dremio-oss/ ./dremio-oss/
WORKDIR /dremio/dremio-oss
RUN mvn -Dmaven.repo.local=/dremio/m2 -T 1C  package -Dmaven.test.skip=true -Dlicense.skip -pl :dremio-dac-backend
RUN mvn -Dmaven.repo.local=/dremio/m2 -T 1C  package -Dmaven.test.skip=true -Dlicense.skip -pl :dremio-distribution -am

FROM openjdk:8-jdk as run
LABEL org.label-schema.name='dremio/dremio-oss'
LABEL org.label-schema.description='Dremio OSS.'
COPY --from=build /dremio/dremio-oss/distribution/server/target/dremio*.tar.gz /dremio.tar.gz

RUN \
  mkdir -p /opt/dremio \
  && mkdir -p /var/lib/dremio \
  && mkdir -p /var/run/dremio \
  && mkdir -p /var/log/dremio \
  && mkdir -p /opt/dremio/data \
  \
  && groupadd --system dremio \
  && useradd --base-dir /var/lib/dremio --system --gid dremio dremio \
  && chown -R dremio:dremio /opt/dremio/data \
  && chown -R dremio:dremio /var/run/dremio \
  && chown -R dremio:dremio /var/log/dremio \
  && chown -R dremio:dremio /var/lib/dremio \
  && tar vxfz dremio.tar.gz -C /opt/dremio --strip-components=1 \
  && rm -rf dremio.tar.gz

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
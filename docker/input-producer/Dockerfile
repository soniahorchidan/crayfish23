# offical Dockerfile template for their Apache Maven images
# reference: https://github.com/carlossg/docker-maven/tree/f99b8cd582524a58a73b23b97e9b8c17001573af/eclipse-temurin-11

FROM eclipse-temurin:11-jdk as builder

ARG MAVEN_VERSION=3.9.3
ARG USER_HOME_DIR="/root"
ARG SHA=400fc5b6d000c158d5ee7937543faa06b6bda8408caa2444a9c947c21472fde0f0b64ac452b8cec8855d528c0335522ed5b6c8f77085811c7e29e1bedbb5daa2
ARG BASE_URL=https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN apt-get update \
  && apt-get install -y ca-certificates curl git gnupg dirmngr --no-install-recommends \
  && rm -rf /var/lib/apt/lists/*
RUN set -eux; curl -fsSLO --compressed ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA} *apache-maven-${MAVEN_VERSION}-bin.tar.gz" | sha512sum -c - \
  && curl -fsSLO --compressed ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz.asc \
  && export GNUPGHOME="$(mktemp -d)"; \
  for key in \
  6A814B1F869C2BBEAB7CB7271A2A1C94BDE89688 \
  29BEA2A645F2D6CED7FB12E02B172E3E156466E8 \
  ; do \
  gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys "$key" ; \
  done; \
  gpg --batch --verify apache-maven-${MAVEN_VERSION}-bin.tar.gz.asc apache-maven-${MAVEN_VERSION}-bin.tar.gz
RUN mkdir -p ${MAVEN_HOME} ${MAVEN_HOME}/ref \
  && tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C ${MAVEN_HOME} --strip-components=1 \
  && ln -s ${MAVEN_HOME}/bin/mvn /usr/bin/mvn
# smoke test
RUN mvn --version


FROM eclipse-temurin:11-jdk

# add user
# USER_ID and GROUP_ID should match the user on the host machine
ARG USER_ID
ARG GROUP_ID
ARG USER_NAME="user"
ARG GROUP_NAME="user"
ARG USER_HOME_DIR="/home/${USER_NAME}"

RUN groupadd -f -g ${GROUP_ID} ${GROUP_NAME} \
  && useradd -l -u ${USER_ID} -g ${GROUP_NAME} ${USER_NAME} \
  && mkhomedir_helper ${USER_NAME}

# maven setup
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "${USER_HOME_DIR}/.m2"
COPY --from=builder ${MAVEN_HOME} ${MAVEN_HOME}
RUN ln -s ${MAVEN_HOME}/bin/mvn /usr/bin/mvn
COPY --chown=${USER_ID}:${GROUP_ID} docker/mvn-entrypoint.sh /usr/local/bin/mvn-entrypoint.sh
COPY --chown=${USER_ID}:${GROUP_ID} docker/settings-docker.xml ${MAVEN_HOME}/ref/

# crayfish external dependencies
## 1. python3, netcat
RUN apt-get update \
  && apt-get install -y ca-certificates curl git python3 netcat --no-install-recommends \
  && rm -rf /var/lib/apt/lists/*

# run as user
USER ${USER_ID}:${GROUP_ID}

# crayfish files
ENV CRAYFISH_HOME="${USER_HOME_DIR}/crayfish"
RUN mkdir -p ${CRAYFISH_HOME}
WORKDIR ${CRAYFISH_HOME}
COPY --chown=${USER_ID}:${GROUP_ID} core core
COPY --chown=${USER_ID}:${GROUP_ID} input-producer input-producer
RUN mkdir -p experiments-driver
COPY --chown=${USER_ID}:${GROUP_ID} experiments-driver/scripts experiments-driver/scripts
RUN mkdir -p experiments-driver/configs
COPY --chown=${USER_ID}:${GROUP_ID} experiments-driver/configs/model-configs experiments-driver/configs/model-configs
COPY --chown=${USER_ID}:${GROUP_ID} docker/input-producer/pom-input-producer.xml pom.xml
COPY --chown=${USER_ID}:${GROUP_ID} log4j.properties ./

## build
RUN /usr/local/bin/mvn-entrypoint.sh
RUN mvn clean install

ENTRYPOINT ["/usr/local/bin/mvn-entrypoint.sh"]
CMD [""]
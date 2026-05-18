# CATMA Standalone Dockerfile
# Sample build command: docker buildx build --platform linux/amd64,linux/arm64 --build-arg CATMA_VERSION=7.2.4 --tag maltem/catma-standalone:7.2.4-gl18.8.4 \
#                       --tag maltem/catma-standalone:latest --build-context srv_assets=C:\Code\catma_standalone_assets \
#                       --build-context build_assets=C:\Code\catma\target --push .
# Consider adding --pull --no-cache as per https://docs.docker.com/build/building/best-practices/#rebuild-your-images-often
#
# Although containers started from this image don't generate much direct output, you may want to use Docker's 'local' logging driver:
# https://docs.docker.com/engine/logging/configure/

# syntax=docker/dockerfile:1
FROM  gitlab/gitlab-ee:18.11.3-ee.0 AS base
LABEL maintainer="CATMA Team" \
      version="1.1.2"

SHELL ["/bin/bash", "-c"]

FROM base AS build-arm64
ARG JDK_FILENAME=jdk-21_linux-aarch64_bin.tar.gz

FROM base AS build-amd64
ARG JDK_FILENAME=jdk-21_linux-x64_bin.tar.gz

FROM build-${TARGETARCH} AS build

# all of these can be overriden with '--build-arg <key>=<value>' at build time
ARG JETTY_FILENAME=jetty-home-12.0.31.tar.gz
# some dirs owned by the jetty user are expected to be bind-mounted, therefore we default to a uid that is less likely to be in use on the host
ARG JETTY_USER_UID=899
ARG CATMA_WAR_FILENAME=catma-7.2-SNAPSHOT.war
ARG CATMA_VERSION=7.2
ENV CATMA_VERSION=$CATMA_VERSION

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y \
         unzip \
         pwgen \
    && apt-get clean -y

# all of these can be overriden with '--env <key>=<value>' at run time
# see bootstrap.sh for how LOW_MEM is handled
ENV LOW_MEM=false
ENV JETTY_JAVA_OPTIONS="-Xms1g -Xmx2g"
# if you override the ports, make sure that the corresponding '--publish' params of the 'docker run' command match as well
# if you override a port *and* the corresponding URL, make sure to include the port in the URL and that it matches 
# if you override GITLAB_URL, make sure that the corresponding '--add-host' param of the 'docker run' command matches as well
# these ports/URLs end up in config (catma.properties, gitlab.rb) and are output by bootstrap.sh - see that file for further details
ENV GITLAB_PORT=8088
ENV GITLAB_URL="http://gitlab.localhost:${GITLAB_PORT}"
ENV CATMA_PORT=8089
ENV CATMA_URL="http://catma.localhost:${CATMA_PORT}"

# expects build to be run with: --build-context srv_assets=</path/to/srv_assets>
# the srv_assets directory should contain: JDK_FILENAME & JETTY_FILENAME
RUN --mount=from=srv_assets,target=/tmp/srv_assets \
    # install Java
    mkdir -p /opt/java/oracle \
    && tar -xzf "/tmp/srv_assets/${JDK_FILENAME}" -C /opt/java/oracle \
    && JDK_DIR=$(find /opt/java/oracle -maxdepth 1 -type d -name 'jdk*' -exec basename {} \; -quit) \
    && update-alternatives --install /usr/bin/java java "/opt/java/oracle/${JDK_DIR}/bin/java" 10 \
                           --slave /usr/bin/javac javac "/opt/java/oracle/${JDK_DIR}/bin/javac" \
    # install and configure Jetty
    && adduser --system --uid $JETTY_USER_UID --group --home /opt/jetty_usr_home --no-create-home jetty \
    && tar -xzf "/tmp/srv_assets/${JETTY_FILENAME}" -C /opt \
    && JETTY_DIR=$(find /opt -maxdepth 1 -type d -name 'jetty-home*' -exec basename {} \; -quit) \
    && mkdir -p /opt/jetty_usr_home/run \
    && mkdir -p /opt/jetty_web/catma_base \
    && chown -R jetty:root "/opt/${JETTY_DIR}" /opt/jetty_usr_home /opt/jetty_web \
    && chown jetty:jetty /opt/jetty_usr_home/run \
    && ln -s "/opt/${JETTY_DIR}" /opt/jetty \
    && su jetty -s /bin/bash -c "cd /opt/jetty_web/catma_base \
                                 && java -jar /opt/jetty/start.jar --create-startd \
                                 && java -jar /opt/jetty/start.jar --add-modules=ee8-deploy,ee8-websocket-javax,http,http2c,console-capture,forwarded,requestlog \
                                 && mkdir webapps/ROOT" \
    && cp /opt/jetty/bin/jetty.sh /etc/init.d/jetty \
    && <<FILE1 cat >> /opt/jetty_web/catma_base/start.d/ee8-deploy.ini \
    && <<FILE2 cat >> /opt/jetty_web/catma_base/start.d/http.ini \
    && <<FILE3 cat >> /opt/jetty_web/catma_base/start.d/console-capture.ini \
    && <<FILE4 cat >> /opt/jetty_web/catma_base/start.d/requestlog.ini \
    && <<FILE5 cat > /etc/default/jetty
jetty.deploy.scanInterval=0
FILE1
jetty.http.host=0.0.0.0
jetty.http.port=${CATMA_PORT}
FILE2
jetty.console-capture.retainDays=30
jetty.console-capture.timezone=UTC
FILE3
jetty.requestlog.retainDays=30
FILE4
JETTY_USER=jetty
JETTY_HOME=/opt/jetty
JETTY_BASE=/opt/jetty_web/catma_base
JETTY_RUN=/opt/jetty_usr_home/run
TMPDIR=/opt/jetty_temp
JAVA_OPTIONS="${JETTY_JAVA_OPTIONS}"
FILE5

# expects build to be run with: --build-context build_assets=</path/to/build_assets> (typically /target relative to the code dir)
# the build_assets directory should contain: CATMA_WAR_FILENAME
RUN --mount=from=build_assets,target=/tmp/build_assets \
    unzip "/tmp/build_assets/${CATMA_WAR_FILENAME}" -d /opt/jetty_web/catma_base/webapps/ROOT/ \
    && chown -R jetty:jetty /opt/jetty_web/catma_base/webapps/ROOT/

COPY --link --parents --chmod=0755 assets scripts templates /opt/catma/
# we *could* also copy the .gitconfig template in the bootstrap script, it's just easier to do it all with one command here
# files copied to bind-mounted locations (the volumes declared below are expected to be bind-mounted), e.g. the catma.properties template, **have** to be copied
# in the bootstrap script, as they would otherwise be obscured by the bind mount
COPY --link --chown=jetty:jetty --chmod=0640 templates/.gitconfig.template /opt/jetty_usr_home/.gitconfig

EXPOSE $GITLAB_PORT $CATMA_PORT

VOLUME ["/opt/jetty_web/catma_base/logs", "/opt/jetty_temp", "/data/catma"]

CMD ["/opt/catma/scripts/bootstrap.sh"]

# CATMA Standalone Dockerfile
# Sample build command: docker buildx build --platform linux/amd64,linux/arm64 --tag maltem/catma-standalone:0.0.1 --tag maltem/catma-standalone:latest \
#                       --build-context srv_assets=C:\Code\catma_standalone_assets --build-context build_assets=C:\Code\catma\target --push .
# Consider adding --no-cache as per https://docs.docker.com/build/building/best-practices/#rebuild-your-images-often
#
# Although containers started from this image don't generate much output, you may want to use Docker's 'local' logging driver:
# https://docs.docker.com/engine/logging/configure/

# syntax=docker/dockerfile:1
FROM  gitlab/gitlab-ee:18.2.8-ee.0
LABEL maintainer="CATMA Team" \
      version="0.0.1"

SHELL ["/bin/bash", "-c"]

# all of these can be overriden with '--build-arg' at build time
ARG JDK_FILENAME=jdk-21_linux-x64_bin.tar.gz
ARG JETTY_FILENAME=jetty-home-12.0.29.tar.gz
ARG CATMA_WAR_FILENAME=catma-7.2-SNAPSHOT.war

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y \
         unzip \
         pwgen \
    && apt-get clean -y

# all of these can be overriden with '--env <key>=<value>' at run time
# see bootstrap.sh for how LOW_MEM is handled
ENV LOW_MEM=false
ENV JETTY_JAVA_OPTIONS="-Xms1g -Xmx2g -server"
# for the port numbers, which are on the **host**, make sure that the left side of the corresponding '--publish' params match as well
# the host ports are used for some of the URLs configured in catma.properties, see bootstrap.sh
ENV CATMA_PORT=8089
ENV GITLAB_PORT=8088

# expects build to be run with: --build-context srv_assets=</path/to/srv_assets>
# the srv_assets directory should contain: JDK_FILENAME & JETTY_FILENAME
RUN --mount=from=srv_assets,target=/tmp/srv_assets \
    # install Java
    mkdir -p /opt/java/oracle \
    && tar -xzf /tmp/srv_assets/${JDK_FILENAME} -C /opt/java/oracle \
    && JDK_DIR=$(find /opt/java/oracle -maxdepth 1 -type d -name 'jdk*' -exec basename {} \; -quit) \
    && update-alternatives --install /usr/bin/java java /opt/java/oracle/${JDK_DIR}/bin/java 10 \
                           --slave /usr/bin/javac javac /opt/java/oracle/${JDK_DIR}/bin/javac \
    # install and configure Jetty
    && adduser --system --group --home /opt/jetty_usr_home --no-create-home jetty \
    && tar -xzf /tmp/srv_assets/${JETTY_FILENAME} -C /opt \
    && JETTY_DIR=$(find /opt -maxdepth 1 -type d -name 'jetty-home*' -exec basename {} \; -quit) \
    && mkdir -p /opt/jetty_usr_home/run \
    && mkdir /opt/jetty_temp \
    # note that in addition to the ownership changes below, ownership of the logs dir is changed in the bootstrap shell script
    && mkdir -p /opt/jetty_web/catma_base/logs \
    && chown -R jetty:root /opt/${JETTY_DIR} /opt/jetty_usr_home /opt/jetty_web \
    && chown jetty:jetty /opt/jetty_usr_home/run \
    && ln -s /opt/${JETTY_DIR} /opt/jetty \
    && su jetty -s /bin/bash -c "cd /opt/jetty_web/catma_base \
                                 && java -jar /opt/jetty/start.jar --create-startd \
                                 && java -jar /opt/jetty/start.jar --add-modules=ee8-deploy,http,console-capture,ee8-websocket-javax \
                                 && mkdir webapps/ROOT" \
    && cp /opt/jetty/bin/jetty.sh /etc/init.d/jetty \
    && <<FILE1 cat >> /opt/jetty_web/catma_base/start.d/console-capture.ini \
    && <<FILE2 cat >> /opt/jetty_web/catma_base/start.d/ee8-deploy.ini \
    && <<FILE3 cat >> /opt/jetty_web/catma_base/start.d/http.ini \
    && <<FILE4 cat > /etc/default/jetty
jetty.console-capture.retainDays=30
jetty.console-capture.timezone=UTC
FILE1
jetty.deploy.scanInterval=0
FILE2
jetty.http.host=0.0.0.0
jetty.http.port=8089
FILE3
JETTY_USER=jetty
JETTY_HOME=/opt/jetty
JETTY_BASE=/opt/jetty_web/catma_base
JETTY_RUN=/opt/jetty_usr_home/run
TMPDIR=/opt/jetty_temp
JAVA_OPTIONS="${JETTY_JAVA_OPTIONS}"
FILE4

# expects build to be run with: --build-context build_assets=</path/to/build_assets> (typically /target relative to the code dir)
# the build_assets directory should contain: CATMA_WAR_FILENAME
RUN --mount=from=build_assets,target=/tmp/build_assets \
    mkdir -p /data/catma/localgit \
    && mkdir /data/catma/localgit_api \
    && unzip /tmp/build_assets/${CATMA_WAR_FILENAME} -d /opt/jetty_web/catma_base/webapps/ROOT/ \
    && cp /opt/jetty_web/catma_base/webapps/ROOT/WEB-INF/classes/catma.db /data/catma/ \
    && chown -R jetty:jetty /data /opt/jetty_web/catma_base/webapps/ROOT/

COPY --link --chown=jetty:jetty --chmod=0640 templates/catma.properties.template /opt/jetty_web/catma_base/webapps/ROOT/catma.properties
COPY --link --chown=jetty:jetty --chmod=0640 templates/.gitconfig.template /opt/jetty_usr_home/.gitconfig
COPY --link --chmod=0755 assets /opt/assets
COPY --link --chmod=0755 scripts /opt/scripts

EXPOSE 80 8089

VOLUME ["/opt/jetty_web/catma_base/logs", "/opt/jetty_temp", "/data/catma"]

CMD ["/opt/scripts/bootstrap.sh"]

#TODOs:
# - check if the default JETTY_JAVA_OPTIONS are reasonable in this context

# Introduction

CATMA is a web based application for text annotation and analysis.

It has two main components: an annotation component that allows the user to annotate text and an analysis component that supports pattern analysis of text and annotations in combination through a custom but easy query language and a data visualization facility based on [Vega](https://vega.github.io/vega/).

It also allows the project-centric management of text corpora, annotation collections, tagsets and team members.

CATMA uses a Gitlab instance as its backend to store and manage the project's resources and team. All resources are versioned and available through either the CATMA web interface or the Gitlab REST API.

# Requirements
 * jdk11 or lower, ideally jdk8
  * say on debian 12/bookworm, you wanna get jdk from oldstable, so you don't have to change libc6
 * maven
 * docker

# Installation & Setup
First, you need to get some dependencies dealt with:
```
git clone https://github.com/forTEXT/serverside-elements.git # some bundled dependency that is patched
cd serverside-elements
mvn compile package
mvn install:install-file -Dfile=elements/target/elements-0.2.3-CATMA.jar -DpomFile=elements/pom.xml
cd ..
git clone https://github.com/forTEXT/gitlab4j-api.git # some other patched dependency
cd gitlab4j-api
mvn compile package
mvn install:install-file -Dfile=target/gitlab4j-api-5.1.0-SNAPSHOT.jar -DpomFile=pom.xml
cd ..
sudo docker pull gitlab/gitlab-ce
echo > docker-compose.yml """
services:
  gitlab-server:
    image: 'gitlab/gitlab-ce'
    container_name: gitlab-server
    ports:
      - '8088:80'
    environment:
      GITLAB_ROOT_EMAIL: 'example@example.com'
      GITLAB_ROOT_PASSWORD: 'Abcd@0123456789 CHANGE THIS'
    volumes:
      - ./gitlab/config:/etc/gitlab
      - ./gitlab/data:/var/opt/gitlab
"""
sudo docker compose up
```

This has downloaded a gitlab docker insancen. When the virtual machine in which gitlab
runs will be started you should be able to configure in your browser at localhost:8088.
You then need to change the configuration this way:
 * The default dev ops pipeline needs to be switched off.
   * That is in settings => CI/CD => Continuous integration and development => remove
     the tickmark before "Default to Auto DevOps pipeline for all projects"
 * The default branch protection needs to be disabled
   * Settings => Repository => Default branch => Initial branch protection needs to be
     set to not protected
 * Then you need to go in your user settings and create and access token for the API
   for the root account
   * User settings => access tokens

```
git clone https://github.com/forTEXT/catma
cd catma
cp src/main/resources/catma.properties .
```

You probably wanna change some config option in catma.properties ; namely, if you
plan on just running catma via maven for testing purposes, you probably want
to set the BASE_URL to http://localhost:8080/catma/. Also you need to configure the
gitlab access token and server url.

Then you can:
```
mvn clean compile package
```
As maven runs tests after the compilation, the gitlab link gets extensively tested,
so you need that link to run so you can compile the catma jar file.

Copy and configure the catma.properties file from the resource folder to the web app root folder.
```
cp catma.properties src/main/webapp/catma.properties
```

CATMA is tested with the jetty servlet container. To run it, you just have to type:
```
mvn jetty:run
```

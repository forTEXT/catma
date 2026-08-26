# Development

These are instructions for setting up a development environment to work on the CATMA codebase, as well as running the application for testing purposes.

## Prerequisites

- JDK 21
- Maven
- A self-managed GitLab server <v19, with full administrator access and that is dedicated to CATMA (for example, using GitLab's Docker image – refer to the
  [self-hosting instructions](SELF-HOSTING.md))

## Installation & Setup

This repository contains a `catma.iml` project file for [IntelliJ IDEA](https://www.jetbrains.com/idea/), its usage of course being optional. Similarly, the
`.run` directory contains run configurations for IDEA's Jetty and Maven plugins, among others. These should help with getting set up if you use IDEA.

Contributors have also used [Eclipse IDE](https://eclipseide.org/) in the past, and nowadays there are many options to choose from as far as IDEs go. The
aforementioned IDEs have good support for the frameworks used in CATMA.

You can skip the next section if you don't use IDEA – the later instructions are generic.

### IntelliJ IDEA

If you use IDEA, there are a few additional points worth noting:
- You'll also need the GWT plugin, though you should automatically be prompted to install it.
- Unfortunately, IDEA's GWT plugin has been [neglected](https://youtrack.jetbrains.com/projects/IDEA/issues/IDEA-379830/Move-GWT-to-obsolete-plugins) and become
  increasingly unreliable. Without going into too much detail, an unfortunate consequence of this (and some other decisions that JetBrains made) is that IDEA
  "forgets" where in the web app artifact the GWT output should go whenever the Maven project is reloaded. See
  [this screenshot](screenshots/intellij-idea-project-structure-artifacts-config.png) for how to configure the artifact in the *Project Structure* settings.
- Unfortunately, IDEA's Jetty plugin has also been neglected and
  [isn't working properly with Jetty versions >12.0.16](https://youtrack.jetbrains.com/issue/IDEA-369022/Jetty-12-EE10-deployment-broken-starting-with-12.0.17).
- The included run configurations assume that a `catma_local-dev.properties` file (copy the [template file](../src/main/resources/catma.properties) and
  configure as needed) exists at a particular location:
  - For the Jetty run configurations: where the built web app artifacts end up (typically `target/catma-x.x-SNAPSHOT/`).
  - For the Maven run configurations: in `src/main/webapp/`.
  - Also note that you need to copy the [SQLite DB](../src/main/resources/catma.db) to the `SQLITE_DB_BASE_PATH` in your properties file, otherwise CATMA won't
    start.
  - See below for further details about the settings that need to be changed in the properties file.

### Forked Dependencies

CATMA currently uses a forked version of `org.vaadin.elements`. You need to install the custom build in your local Maven repository using
`mvn install:install-file -Dfile=<path-to-dependency-jar-file> -DpomFile=<path-to-dependency-pom-file>` (the installation command is also shown in the next
section). The CATMA build won't succeed otherwise!

You can find the pre-built JAR file and associated pom.xml file here:
- serverside-elements - [JAR](https://github.com/forTEXT/serverside-elements/releases/download/0.2.3-CATMA/elements-0.2.3-CATMA.jar)
  | [pom.xml](https://github.com/forTEXT/serverside-elements/raw/refs/tags/0.2.3-CATMA/elements/pom.xml)

Note that `gitlab4j-api` used to be forked as well, but we now use the stock release from Maven Central. Stay on the 5.x line - 6.x requires Jakarta EE
(`jakarta.ws.rs`, Jersey 3.x) and is incompatible with CATMA's javax-based stack (Vaadin 8, Jersey 2.x, `javax.servlet`).

### Building & Running for Testing Purposes

These are the generic steps to build CATMA and to run it via Maven on a Linux system. Your GitLab server should be running and configured already (see
[prerequisites](#prerequisites)). The JDK and Maven installations are included below.

Note that we are skipping the automated tests here – this is because a number of them test interactions with the GitLab server (setting up data, performing the
test, and cleaning up again) and are quite slow as a result.

```
apt update && apt install openjdk-21-jdk maven git curl

curl -L https://github.com/forTEXT/serverside-elements/releases/download/0.2.3-CATMA/elements-0.2.3-CATMA.jar -o elements-0.2.3-CATMA.jar
curl -L https://github.com/forTEXT/serverside-elements/raw/refs/tags/0.2.3-CATMA/elements/pom.xml -o elements-0.2.3-CATMA.pom.xml

mvn install:install-file -Dfile=elements-0.2.3-CATMA.jar -DpomFile=elements-0.2.3-CATMA.pom.xml

git clone https://github.com/forTEXT/catma
cd catma
mvn clean compile package -DskipTests=true

mkdir -p temp db repo/git repo/git_api
cp src/main/resources/catma.db db/
cp src/main/resources/catma.properties src/main/webapp/
```

Before you can start CATMA, you will need to change some settings in the `catma.properties` file that you copied above. At a minimum, you should set the
following (adjust for your environment):
```
BASE_URL=http://localhost:8080/
TEMP_DIR=/your/path/to/catma/temp
GITLAB_SERVER_URL=http://localhost:8088
GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN=your-admin-personal-access-token
GITLAB_OAUTH_CLIENT_ID=your-oauth-application-id
GITLAB_OAUTH_CLIENT_SECRET=your-oauth-application-secret
GIT_REPOSITORY_BASE_PATH=/your/path/to/catma/repo/git
SQLITE_DB_BASE_PATH=/your/path/to/catma/db
RESET_PASSWORD_URL=http://localhost:8088/users/password/new
LOGOUT_URL=http://localhost:8080/
API_GIT_REPOSITORY_BASE_PATH=/your/path/to/catma/repo/git_api
API_HMAC_SECRET=your-long-and-random-secret
```

Finally, you can simply run the following to start the Jetty server:
`mvn jetty:run`

#### The GitLab OAuth Application

Signing in to CATMA is an OAuth authorization code flow against your GitLab server, so you have to register an application there before anyone can log in.
In GitLab, go to *Admin → Applications → New application* and set:
- **Name**: CATMA
- **Redirect URI** (one per line):
  ```
  http://localhost:8080/
  http://localhost:8080/api/v1/auth/gitlab/callback
  ```
  These must match `BASE_URL` exactly (including the trailing slash), plus the same value with `api/v1/auth/gitlab/callback` appended for the REST API.
- **Confidential**: checked
- **Trusted**: checked - this skips the authorization (consent) screen, which is appropriate because CATMA is a first-party application
- **Scopes**: `api` and `write_repository`

Copy the resulting *Application ID* and *Secret* into `GITLAB_OAUTH_CLIENT_ID` and `GITLAB_OAUTH_CLIENT_SECRET`. The secret is stored hashed by GitLab and is
only shown immediately after creation.

Note that this replaces the resource owner password credentials (ROPC) grant that CATMA used previously, which GitLab removed in version 19.0. Users now enter
their credentials on GitLab's own login page rather than in CATMA, which is why *Settings → General → Sign-in restrictions → Allow password and passkey
authentication for the web interface* must remain enabled.

#### Additional Notes

To enable **user registration**, you will also need to set the `MAIL_*`, `GOOGLE_RECAPTCHA_*` and `SIGNUP_TOKEN_KEY` properties. Alternatively, you can log in
to CATMA with any regular email/password-based user account that exists on your GitLab server. By default, creating user accounts using the GitLab admin
interface requires GitLab to be configured to be able to send emails, because email confirmation is a strict requirement. You can change the email confirmation
settings at: *Settings → General → New user account restrictions*. User accounts can also be created directly using the
[GitLab API](https://docs.gitlab.com/api/users/#create-a-user).

To enable **Google account logins**, you will also need to set the `GOOGLE_OAUTH_*` properties and configure the GitLab OmniAuth settings in GitLab's
`gitlab.rb` configuration file, as shown [here](../docker/README.md#multi-user).

An internet connection is not strictly necessary if you are running everything locally, however external mail or Google reCAPTCHA / OAuth will obviously not
work without one.

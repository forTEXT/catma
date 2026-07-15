# Host Your Own Instance

To host your own CATMA instance, you need a Java web server and servlet container, and you also need to set up your own GitLab server. As the setup and
configuration can be complicated, we highly recommend that you use our provided Docker image – the [standalone version](../docker/README.md).

If you don't want to use the Docker image, continue reading for further self-hosting instructions. Also note that the [Dockerfile](../docker/Dockerfile) and
associated [bootstrapping scripts](../docker/scripts) still serve as a good blueprint for how to set things up.

## Java Web Server and Servlet Container

We choose to use [Eclipse Jetty](https://jetty.org/), but a modern alternative like, for example, [Apache Tomcat](https://tomcat.apache.org/) should also work.

## GitLab Server

You will need to install the GitLab server using the [cloud native or Linux package installation methods](https://about.gitlab.com/install/#cloud-native) (full
administrator access is required). Also note that:
- GitLab 19+ is not yet supported by CATMA
- There are some configuration changes that you need to make within GitLab before CATMA will work properly. The necessary changes are listed in the [GitLab
  configuration Ruby script](../docker/scripts/gitlab_config.rb#L54-L66). This script can also be run independently on the GitLab server using
  `gitlab-rails runner` (usage hint [here](../docker/scripts/gitlab_config.rb#L29)). Alternatively, you can manually make the changes via the GitLab Admin UI:
    - Turn **ON**: Settings → General → Sign-in restrictions → Allow password and passkey authentication for the web interface
    - Turn **OFF**: Settings → CI/CD → Continuous Integration and Deployment → Default to Auto DevOps pipeline for all projects
    - Set the default branch name to "**master**": Settings → Repository → Default branch → Initial default branch name
    - Change the branch protection defaults: Settings → Repository → Default branch → Initial default branch protection → **Protected**:
        - Allowed to push → **Developers + Maintainers**
        - Allowed to merge → **Maintainers**
        - Turn **OFF**: Allowed to force push
        - Turn **OFF**: Allow developers to push to the initial commit

### Create a Personal Access Token for the Admin Account

The CATMA application communicates with the GitLab backend via GitLab's API. As certain operations occur outside the context of a particular CATMA user and/or
require administrative access, CATMA needs to be configured with a personal access token for the admin account. To create this token, navigate to the user
preferences for the admin user, select *Access → Personal access tokens* from the menu on the left and create a new token with the **api** scope.

Note that tokens have an expiration date by default. It is considered good security practice to regularly rotate tokens; however, there is an option that will
allow you to create tokens without expiration (Settings → General → Account and limit → Access token expiration).

## Application Deployment

Refer to the [development documentation](DEVELOPMENT.md) for instructions on how to build the application. The build produces a `.war` file that can be used for
deployment.

The exact deployment steps will depend on the Java web server and servlet container that you choose. If you choose Jetty, you can refer to the Dockerfile
and associated bootstrapping scripts as mentioned at the top of this page.

You will also need to:
1. Create a `catma.properties` file based on the [template](../src/main/resources/catma.properties) and change some settings according to your environment. The
   development documentation linked to above contains further details about the settings that need to be changed. The properties file needs to be placed within
   the `.war` file (it's really just a ZIP archive) or in the same location that you extracted the `.war` file to, depending on your exact deployment scenario.
2. Copy the [SQLite DB](../src/main/resources/catma.db) to the `SQLITE_DB_BASE_PATH` you set in your properties file, otherwise CATMA won't start.

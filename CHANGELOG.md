# Changelog

Notable changes to CATMA, and the steps needed to update an existing installation. Each version has separate sections for
[self-hosted](doc/SELF-HOSTING.md) installations and for [CATMA Standalone](docker/README.md) (the Docker image). Read the section for your scenario before
updating, and apply the steps for every version between the one you are running and the one you are updating to.

## 7.4.0

### General

- Users now sign in on the GitLab backend's own login page, from which they are redirected back to CATMA. CATMA no longer handles passwords itself.
- Sign-in with Google is now provided by GitLab, as a button on that same login page. Accounts are still created through CATMA, and accounts created by
  CATMA's former Google sign-in need no migration – they are linked by email address the first time their owner signs in with Google.
- GitLab 19 is now supported. GitLab 19.0 removed the "external" password-based sign-in that earlier CATMA versions relied on, which is what made the changes
  above necessary.

**Breaking REST API changes:**
- `/api/v1/auth` no longer accepts a username and password (HTTP Basic authentication). Pass a GitLab access token instead (bearer authentication or the
  `access_token` form parameter), or use the browser-based flow below.
- The browser-based authentication endpoint has moved from `/api/v1/auth/google` to `/api/v1/auth/gitlab`, and now signs users in through GitLab.

We are not releasing a new API version for these changes: they are a consequence of GitLab's removal of the "external" password-based sign-in, so the old
behaviour can't be offered anymore.

### Self-Hosting

Update GitLab together with CATMA – older GitLab versions have known security vulnerabilities. Follow GitLab's
[upgrade path](https://gitlab-com.gitlab.io/support/toolbox/upgrade-path/), which may include required intermediate versions, and wait for all
[background migrations](https://docs.gitlab.com/update/background_migrations/#check-for-pending-database-background-migrations) to finish before each
subsequent step.

Then, before deploying the new CATMA version:

1. **Register the OAuth application** that users sign in through, as described under
   [Create the OAuth Application](doc/SELF-HOSTING.md#create-the-oauth-application), and add its credentials to your `catma.properties` file as two new
   properties: `GITLAB_OAUTH_CLIENT_ID` and `GITLAB_OAUTH_CLIENT_SECRET`. CATMA checks the application at startup and refuses to start if it is missing or
   misconfigured, naming the problem in the servlet container log.
2. **Replace the admin token** if it doesn't have the `sudo` scope, which is now required in addition to `api`. Scopes can't be added to an existing token, so
   create a new one as described under
   [Create a Personal Access Token for the Admin Account](doc/SELF-HOSTING.md#create-a-personal-access-token-for-the-admin-account) and update the
   `GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN` property. CATMA checks the token at startup and refuses to start if a scope is missing, naming it in the servlet
   container log.
3. **If you offer Google sign-in**, move it to GitLab as described under [Google Sign-In (Optional)](doc/SELF-HOSTING.md#google-sign-in-optional). In
   particular, `gitlab_rails['omniauth_enabled'] = true` is new, and `omniauth_allow_single_sign_on`, `omniauth_sync_profile_from_provider` and
   `omniauth_sync_profile_attributes` should be removed from `gitlab.rb` if you had set them – the section explains why.
4. **Remove obsolete properties** from your `catma.properties` file: `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET` and `RESET_PASSWORD_URL`.

We also recommend that you update GitLab's appearance settings (*Admin → Settings → Appearance*), because users now see GitLab's login page every time they
sign in to CATMA, and its description should tell them where they are and why. You can use the
[GitLab configuration Ruby script](docker/scripts/gitlab_config.rb#L129-L159) as a reference: besides the description, it sets the site name and logos, and
links to CATMA's terms of use and privacy policy from GitLab's terms of service (*Admin → Settings → General → Terms of Service and Privacy Policy*).

### Docker (Standalone)

The image is now based on GitLab 19.4.1 (`7.4.0-gl19.4.1`). It now uses OpenJDK instead of Oracle's JDK, and an updated version of Jetty.

Follow the usual [update process](docker/README.md#update-process). GitLab requires an intermediate step: update to `7.4.0-gl19.2.7` first, wait for all
background migrations to finish, and only then update to `7.4.0-gl19.4.1`.

Once you are running `7.4.0-gl19.4.1`, complete the following steps. In the paths below, `/etc/gitlab/` corresponds to `$GITLAB_HOME/config/` on the host and
`/data/catma/` to `$CATMA_HOME/data/` if you use bind mounts as described under [Server Deployment](docker/README.md#server-deployment). Otherwise, edit the
files within the container (e.g. after running `docker exec -it catma-standalone /bin/bash`).

1. **Register the OAuth application** that users sign in through. Sign in to GitLab as `root` and follow the steps under
   [Create the OAuth Application](doc/SELF-HOSTING.md#create-the-oauth-application). The redirect URIs are based on your `CATMA_URL` – for the default
   `http://catma.localhost:8089`:
   ```
   http://catma.localhost:8089/
   http://catma.localhost:8089/api/v1/auth/gitlab/callback
   ```
   Then add the credentials to `/data/catma/catma.properties` as two new lines (your existing file predates these properties, so they won't be there yet):
   ```
   GITLAB_OAUTH_CLIENT_ID=<application-id>
   GITLAB_OAUTH_CLIENT_SECRET=<secret>
   ```
   CATMA checks the application when it starts (step 4) and refuses to start if it is missing or misconfigured, naming the problem in the Jetty log
   (`/opt/jetty_web/catma_base/logs/` in the container, `$CATMA_HOME/logs/` on the host with bind mounts).
2. **Fix the `external_url` setting** in `/etc/gitlab/gitlab.rb`. Earlier versions of the image wrote it as an assignment, which GitLab ignores. Replace the
   line `external_url = '<your-gitlab-url>'` with `external_url '<your-gitlab-url>'` (no equals sign). The line `nginx['listen_port'] = ...` just below it is
   no longer needed if your `GITLAB_URL` includes the port (as the default `http://gitlab.localhost:8088` does), and you can optionally remove it.

   If instead your `GITLAB_URL` is an `https://` URL behind a reverse proxy, you still need to set the listen port: now that `external_url` takes effect,
   GitLab would otherwise try to terminate SSL itself. Replace the line with the settings listed under
   [Exposing the Services](docker/README.md#exposing-the-services), which use the `gitlab_rails['nginx'][...]` keys that GitLab has used for these settings
   since version 19.2 (the top-level `nginx[...]` keys still work, but are deprecated).
3. **If you offer Google sign-in**, update the OmniAuth settings in `/etc/gitlab/gitlab.rb` as shown under [Multi-User](docker/README.md#multi-user): add
   `gitlab_rails['omniauth_enabled'] = true`, and remove the `omniauth_allow_single_sign_on`, `omniauth_sync_profile_from_provider` and
   `omniauth_sync_profile_attributes` lines. Then remove `GOOGLE_OAUTH_CLIENT_ID` and `GOOGLE_OAUTH_CLIENT_SECRET` from `/data/catma/catma.properties`.
4. **Restart the container** (e.g. `docker restart catma-standalone`) so that both GitLab and CATMA pick up the changes.

Optionally, you can remove the obsolete `RESET_PASSWORD_URL` line from `/data/catma/catma.properties`. The admin token needs no changes, as the image has
always created it with the required scopes.

We also recommend updating GitLab's appearance settings, as described at the end of the [Self-Hosting](#self-hosting) section above. The logos that the
configuration script refers to can be found in [docker/assets](docker/assets).

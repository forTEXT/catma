# Host Your Own Instance

To host your own CATMA instance, you need a Java web server and servlet container, and you also need to set up your own GitLab server. As the setup and
configuration can be complicated, we highly recommend that you use our provided Docker image – the [standalone version](../docker/README.md).

If you don't want to use the Docker image, continue reading for further self-hosting instructions. Also note that the [Dockerfile](../docker/Dockerfile) and
associated [bootstrapping scripts](../docker/scripts) still serve as a good blueprint for how to set things up.

## Java Web Server and Servlet Container

We choose to use [Eclipse Jetty](https://jetty.org/), but a modern alternative like, for example, [Apache Tomcat](https://tomcat.apache.org/) should also work.

## GitLab Server

You will need to install the GitLab server using the [cloud native or Linux package installation methods](https://about.gitlab.com/install/#cloud-native) (full
administrator access is required).

Also note that there are some configuration changes that you need to make within GitLab before CATMA will work properly. The necessary changes are listed in the
[GitLab configuration Ruby script](../docker/scripts/gitlab_config.rb#L56-L68) for the Docker image. This script can also be run independently on the GitLab
server using `gitlab-rails runner` (usage hint [here](../docker/scripts/gitlab_config.rb#L29)). You should delete the OAuth application credentials file (see
the `oauth_creds_path` parameter) that the script creates, once you have retrieved the credentials and placed them in your `catma.properties` file as described
under [Application Deployment](#application-deployment) below.

Alternatively, you can manually make the changes via the GitLab Admin UI:
- Turn **OFF**: Settings → General → New user account restrictions → Allow new user accounts
- Turn **ON**: Settings → General → Sign-in restrictions → Allow password and passkey authentication for the web interface
- Turn **OFF**: Settings → CI/CD → Continuous Integration and Deployment → Default to Auto DevOps pipeline for all projects
- Set the default branch name to "**master**": Settings → Repository → Default branch → Initial default branch name
- Change the branch protection defaults: Settings → Repository → Default branch → Initial default branch protection → **Protected**:
    - Allowed to push → **Developers + Maintainers**
    - Allowed to merge → **Maintainers**
    - Turn **OFF**: Allowed to force push
    - Turn **OFF**: Allow developers to push to the initial commit
- Set up the required admin token, as described under
  [Create a Personal Access Token for the Admin Account](#create-a-personal-access-token-for-the-admin-account) below
- Register the OAuth application that users sign in through, as described under [Create the OAuth Application](#create-the-oauth-application) below

*Note that these manual steps only cover those settings that are absolutely necessary for CATMA to work. There are many others that will improve the user
experience – refer to the [GitLab configuration Ruby script](../docker/scripts/gitlab_config.rb#L55) and the
[bootstrap shell script](../docker/scripts/bootstrap.sh#L129-L138).*

### Create a Personal Access Token for the Admin Account

The CATMA application communicates with the GitLab backend via GitLab's API. As certain operations occur outside the context of a particular CATMA user and/or
require administrative access, CATMA needs to be configured with a personal access token for the admin account.

Create the token using the Ruby script mentioned above, or manually by navigating to the user preferences for the admin user, selecting *Access → Personal
access tokens* from the menu on the left and creating a new token with the **api** and **sudo** scopes. Copy the token into the
`GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN` property in your `catma.properties` file (see [Application Deployment](#application-deployment) below).

> **Upgrading an existing installation:** the `sudo` scope is a new requirement. CATMA acts as the new user to disable their notifications during account
> creation, which used to be done with an impersonation token and is now done with `sudo`. Scopes can't be added to an existing token, so you have to create a
> replacement token and update the property – otherwise nobody will be able to sign up.
>
> CATMA checks the token's scopes at startup and **refuses to start** if they are insufficient, naming what is missing in the servlet container log, so this
> is not something you can deploy and only discover later. (If the GitLab server can't be reached at all, CATMA logs that and starts anyway – it will pick
> the connection up on its own.)

Note that tokens have an expiration date by default. It is considered good security practice to regularly rotate tokens; however, there is an option that will
allow you to create tokens without expiration (*Settings → General → Account and limit → Access token expiration*).

### Create the OAuth Application

Users sign in to CATMA via an OAuth authorization code flow against your GitLab server, entering their credentials on GitLab's own login page. (This replaces
the resource owner password credentials (ROPC) grant that CATMA used previously, which GitLab removed in version 19.0.) You therefore have to register an
instance-wide OAuth application before anyone can log in.

Do this using the Ruby script mentioned above, or manually as follows:

Navigate to *Admin → Applications → New application* and set:
- **Name**: CATMA
- **Redirect URI**, one per line – the first must match your `BASE_URL` property exactly, including the trailing slash:
  ```
  https://your-catma-instance.tld/
  https://your-catma-instance.tld/api/v1/auth/gitlab/callback
  ```
  The second entry is for the REST API's browser-based authentication endpoint (`/api/v1/auth/gitlab`).
- **Confidential**: checked
- **Trusted**: checked – this skips the authorization (consent) screen for all users, which is appropriate because CATMA is a first-party application. The
  *Trusted* option is only available for instance-wide applications created in the Admin area.
- **Scopes**: `api`

Copy the resulting *Application ID* and *Secret* into the `GITLAB_OAUTH_CLIENT_ID` and `GITLAB_OAUTH_CLIENT_SECRET` properties. Note that GitLab stores
application secrets hashed, so the secret is only available immediately after creating the application – if you lose it you have to renew it.

### Google Sign-In (Optional)

CATMA has a single sign-in flow, the one described above. Google is offered by GitLab as an OmniAuth provider, so users see it as a button on GitLab's login
page. It is a way of signing in to an account that already exists – account creation stays with CATMA.

Add the following to `gitlab.rb` and run `gitlab-ctl reconfigure`:

```ruby
gitlab_rails['omniauth_enabled'] = true
gitlab_rails['omniauth_auto_link_user'] = ['google_oauth2']   # link to the existing account by email address
gitlab_rails['omniauth_providers'] = [
  { "name" => "google_oauth2", "app_id" => "<client-id>", "app_secret" => "<client-secret>",
    "args" => { "access_type" => "offline", "approval_prompt" => "" } }
]
```

The credentials are those of a Google Cloud OAuth client (see
[Google's documentation](https://developers.google.com/identity/openid-connect/openid-connect#appsetup)). Add
`<GITLAB_SERVER_URL>/users/auth/google_oauth2/callback` to its authorized redirect URIs.

Two settings are deliberately absent:

- **`omniauth_allow_single_sign_on`** should be left unset (its default). It, and *not* the instance-wide `signup_enabled` application setting, is what governs
  account creation through OmniAuth. Leaving it unset forces all accounts to be created through CATMA; with it set, any Google account can create itself an
  account directly in GitLab. Existing accounts are still found and linked to the OAuth identity as described below. A Google account with no matching GitLab
  account is refused with *"Signing in using your Google account without a pre-existing GitLab account is not allowed"*. `omniauth_block_auto_created_users`
  (also unset, defaulting to `true`) is a second line of defense: it parks any auto-created user in a pending-approval state rather than letting them in.
- **`omniauth_sync_profile_from_provider` / `omniauth_sync_profile_attributes`** should both be left unset (their default). With email syncing enabled, GitLab
  rewrites the user's primary email from Google on every sign-in and marks it synced, which makes the field read-only in GitLab's profile UI. Because password
  sign-in matches the username or the *primary* email only, a Google-side address change would then silently invalidate the user's old email as a login
  identifier, with no way for them to undo it. If you have previously enabled these, removing the provider from `sync_profile_from_provider` restores control
  immediately, including for already-linked users.

#### Existing Accounts and Linking

- Accounts created by CATMA's former Google sign-in flow (username `<google-sub>google_com`) need **no migration**. Auto-link matches them by email address on
  the first Google sign-in and attaches a `google_oauth2` identity. The username – and with it the local working-copy path and all project memberships – is
  untouched.
- Email matching is a **first-link-only** mechanism. Once the identity exists, GitLab matches on Google's stable `sub` and either side's address can change
  without breaking sign-in.
- A user whose CATMA account email address differs from their Google address has to link the two themselves, in GitLab's own account settings – auto-link
  matches the primary email only and CATMA has no UI for it.
- Linking doesn't affect password sign-in: the identity and the password are independent credentials, and CATMA-created accounts always have a password.

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

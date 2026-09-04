# TODO (AI-generated)

Work that was identified while working with an AI coding assistant but not done, and that no plan currently covers. Add to this file rather than
leaving the note in a source comment or in documentation aimed at operators.

Entries are grouped by the branch or task they came out of, so that they can be judged in the context that produced them. Add a new section when
working on a branch that has none yet, and drop a section once its items are done or no longer apply.

## `gitlab19-auth-compatibility` — GitLab 19 compatibility and the OAuth sign-in flow

### Terminate the session when an OAuth token refresh fails permanently

`GitLabOauthTokenProvider` backs off after a failed refresh, but it can't recover from one: GitLab invalidates the old refresh token as soon as it issues a
new pair, so a response we never receive leaves the session holding a refresh token that will never work again. Every subsequent API call and Git operation
then fails with a 401 that the user can only escape by signing out and back in, with nothing telling them to.

The session should instead be invalidated and the user prompted to sign in again. That needs a way to distinguish a permanent failure (GitLab answers
`invalid_grant`) from a transient one, and a path from the provider — which is called from background threads as well as the UI thread — to something that
can end the Vaadin session.

### Scale the OAuth token refresh leeway to the token's lifetime

`GitLabOauthTokenProvider` refreshes when the access token is within a fixed `REFRESH_LEEWAY_SECONDS` (60) of expiring. If an instance is ever configured
with an OAuth token lifetime at or below that, the condition is true from the moment the token is issued, so every gitlab4j request and every JGit
credentials lookup mints a new token. Successful refreshes don't arm the failure back-off, so nothing damps it.

No realistic instance runs a lifetime that short — GitLab's default is 2 hours — so this is about degrading sensibly rather than a bug we've hit. Deriving
the leeway from the lifetime, e.g. `min(60, expiresIn / 2)`, would be enough.

NB: Doorkeeper carries `expires_in` over to the token it mints on refresh instead of re-reading the configured default, so a lifetime that was low when a
session started stays low for that session no matter what the setting is changed to afterwards.

### Don't discard invitation parameters when account creation is abandoned

`CreateUserDialog.close()` calls `Page.replaceState(BASE_URL)` unconditionally, which is right after an account signup token has been consumed. When the
dialog was opened from a group or project invitation, however, `RequestTokenHandler` deliberately keeps that token alive so the invitation can still be
accepted, and cancelling or failing out of the dialog now strips `?action=…&token=…` from the URL. The token remains valid in the cache, but the user has no
way back to it short of the original invitation email.

`close()` should only reset the URL for the `verify` action, mirroring what the success path already does when it decides whether to carry the parameters
through the OAuth flow.

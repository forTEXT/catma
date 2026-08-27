# TODO

Work that has been identified but not done, and that no plan currently covers. Add to this file rather than leaving the note in a source comment or in
documentation aimed at operators.

## Terminate the session when an OAuth token refresh fails permanently

`GitLabOauthTokenProvider` backs off after a failed refresh, but it can't recover from one: GitLab invalidates the old refresh token as soon as it issues a
new pair, so a response we never receive leaves the session holding a refresh token that will never work again. Every subsequent API call and Git operation
then fails with a 401 that the user can only escape by signing out and back in, with nothing telling them to.

The session should instead be invalidated and the user prompted to sign in again. That needs a way to distinguish a permanent failure (GitLab answers
`invalid_grant`) from a transient one, and a path from the provider — which is called from background threads as well as the UI thread — to something that
can end the Vaadin session.

## Don't discard invitation parameters when account creation is abandoned

`CreateUserDialog.close()` calls `Page.replaceState(BASE_URL)` unconditionally, which is right after an account signup token has been consumed. When the
dialog was opened from a group or project invitation, however, `RequestTokenHandler` deliberately keeps that token alive so the invitation can still be
accepted, and cancelling or failing out of the dialog now strips `?action=…&token=…` from the URL. The token remains valid in the cache, but the user has no
way back to it short of the original invitation email.

`close()` should only reset the URL for the `verify` action, mirroring what the success path already does when it decides whether to carry the parameters
through the OAuth flow.

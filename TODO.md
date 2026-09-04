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

### Report why a synchronization couldn't complete its merge

`GitProjectHandler.synchronizeWithRemote` returns `false` from two places that mean different things to the user: the merge request can't be merged because
of conflicts they have to resolve in GitLab, and the merge request didn't end up merged although it could have been, which includes GitLab-side states like
`checking` that are worth simply retrying. `GraphWorktreeProject` passes the boolean through to `openProjectListener.ready(null)`, so both produce the same
notification, which hedges across the two.

Carrying a small result type instead of the boolean — merged, needs manual resolution, not completed — would let the notification say which of the two
happened and what to do about it. Genuine failures already take a separate route via the `ExecutionListener`'s error method.

### Stop logging the same background failure twice, and always log who it happened to

Any failure in a `BackgroundService` callable is logged twice: once by `UIBackgroundService` (a bare `"Error"` plus the stack trace) and again by
`CatmaApplication.showAndLogError`, when the `ExecutionListener` reports it. That applies to 7 of the 19 `error` implementations, across roughly 15 `submit`
call sites in analysis, annotation, the project view and the document wizard.

Neither of the two is redundant as things stand. `showAndLogError` is the only one carrying the username and the message the user was shown, and the
`UIBackgroundService` entry is the only record when the UI has detached, because `listener.error` is skipped in that case.

Two things would also make the log easier to follow:

- `UIBackgroundService` logs a bare `"Error"`, so there is nothing tying it to the entry that `showAndLogError` writes for the same incident. Including the
  callable's class would be enough.
- The username should be logged wherever it can be determined, not only by `showAndLogError`. Operators otherwise can't tell which user an entry belongs to
  on an instance with several people working at once.

Worth knowing when changing any of this: `ErrorDialog` renders the message passed to `showAndLogError` followed by `exception.getMessage()` - the outermost
message only. Whatever an exception is wrapped in on its way up is therefore the only exception text the user ever sees, and a null message renders as
nothing at all.

### Attach the project context to synchronization failures without a third log entry

`GraphWorktreeProject$19.call` logs the project name, ID and user before rethrowing, purely to record which project a failed synchronization belongs to.
That makes three entries for one incident rather than the two described above.

Wrapping the exception with that context instead is the obvious replacement and is the wrong one here: the context is of no use to a user who knows which
project they are in, and by the note above it would displace `"GitLab's token endpoint did not return an access token..."`, or whatever else went wrong,
from the dialog - which is the part they are asked to include in a bug report.

### Don't discard invitation parameters when account creation is abandoned

`CreateUserDialog.close()` calls `Page.replaceState(BASE_URL)` unconditionally, which is right after an account signup token has been consumed. When the
dialog was opened from a group or project invitation, however, `RequestTokenHandler` deliberately keeps that token alive so the invitation can still be
accepted, and cancelling or failing out of the dialog now strips `?action=…&token=…` from the URL. The token remains valid in the cache, but the user has no
way back to it short of the original invitation email.

`close()` should only reset the URL for the `verify` action, mirroring what the success path already does when it decides whether to carry the parameters
through the OAuth flow.

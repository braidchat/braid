## 16 April 2021

Search system is now extensible via the following hooks:

Clojure-side:
 - `braid.search.api/register-search-function!`
 - `braid.search.api/register-search-auth-check!`

Clojurescript-side:
 - `braid.search.api/register-search-results-view!`

See the function docstrings for more details

## 1 March 2021

Now uses SMTP to send emails instead of Mailgun's HTTP API.
To this end, now uses the following config variables instead of `:mailgun-domain` and `:mailgun-password`:

 - `:email-host`
 - `:email-user`
 - `:email-password`
 - `:email-port`
 - `:email-from`
 - `:email-secure` (which will can be omitted, `"ssl"` or `"tls"`)

## 27 January 2021

  - Recent large architectural changes:
    - `braid.core/start!` now has an arity-two form that takes the configuration as an explicit map, instead of from environment variables. In this form, the AWS credentials should be in a single key `:aws/credentials-provider` which should be a zero-argument function which returns a vector of access key, secret key, and optionally a session key.
    - The mobile implementation was very bitrotted and has been removed
    - The separate desktop & API servers have been merged; API routes are now on the same server as the desktop with the prefix `/api`

## 16 April 2017

  - Bot configurations are now editable after creation
  - Bot message creation via PUT is now deprecated; use POST instead
  - Bots can opt to receive group events
  - :braid.client/group-add event now includes the id of the group that the user joined (in addition to the user)

## 27 February 2017

  - The Braid client will now detect abnormal disconnects (ex. wifi drops) and indicate when it is disconnected (and when the session cookie expires)

## 8 September 2016

  - It is now possible to tag a thread using the mouse & clicking the "+" button on a thread
  - Braid now uses Elasticsearch for full-text search if available

## 21 August 2016

  - Quests have been added! These are little "achievments" that can help people learn to use braid.  More to come soon!
  - Fix bug that prevented admins from deleting tags
  - Fix bug that prevented updates to tag descriptions from being persisted

## 2 August 2016

  - Fix bug that made thread "limbo" and "private" headers not go away

## 1 August 2016

  - Expand bots API to allow bots to subscribe to threads & look up user nicknames

## 22 July 2016

  - Add the ability to get permalink to a thread

## 21 July 2016

  - Add ability to log in and register (via link or to public groups) with a GitHub account

## 20 July 2016

  - Fix bug that prevented file add button in reply field from working

## 18 July 2016

  - Prevent tag box in autocomplete from collapsing (and highlighting looking weird) when the tag has no description

## 16 July 2016

  - Show changelog in app

## 14 July 2016

  - Thread close keyboard shortcuts now triggers on key down instead of key up (to prevent confusing interactions with autocomplete key handlers)
  - Show user availability status in autocomplete

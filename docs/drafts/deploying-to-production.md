# Deploying Braid to Production

WIP

### Running in `:prod`

Start Redis running (used for cookies & invite tokens).  Braid currently assumes Redis is running on localhost on port 6379.

### Using Datomic Pro

To run Datomic Pro locally, you will need to:

  - sign up for an account with Datomic
  - download the pro version
  - unzip and run the transactor similarly as with the free version (but different template file)
  - add the following to the :chat lein profile (in your profiles.clj):

```clojure
  {:datomic-pro
    [:prod
    {:env {:db-url "datomic:dev://localhost:4333/braid"}}
     :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                      :creds :gpg
                                      :username "USERNAME-HERE"
                                      :password "PASSWORD-HERE"}}}]}
```

Henceforth, you will need to run lein with the profile:

  ```bash
  lein with-profile +datomic-pro repl
  ```

## Hosted Version of Braid

If you want want to use Braid but avoid the hassle of deploying and maintaining a server, the creators of Braid offer a hosted version: http://www.braidchat.com



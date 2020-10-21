# Setting Up Braid On-Prem

This guide explains how to setup your own "production" instance of Braid running on your own server, supporting several groups and users.


## Building an Uberjar

"Building an uberjar" packages Braid and all of it's Clojure and Java dependencies into a single jar file.

You can: (a) build locally and then scp the jar file to a server; or, (b) build directly on a server.

To build, you will need Java8+ and Leiningen on the system you are building.

1. Clone the repo `git clone git@github.com:braidchat/braid.git`
2. `cd braid`
3. `lein uberjar`
4. (wait)
5. Once done, a jar file should be created at: `./target/braid-0.0.1-standalone.jar`
   (if you are building locally, this is the file to scp to your server)


## Dependencies for your Server

To run Braid, your system just needs Java8+ installed.

You can check if java is install by running: `java -version`


## Setting Up Datomic

Braid uses Datomic as it's database. You have several options for how to use Datomic:

Datomic...       | storage                   | cost       | reccommended for...     | notes
-----------------|---------------------------|------------|-------------------------|-------------------
On-Prem Free     | memory store              | free       | development             | all data is lost after server restart
On-Prem Free     | basic disk store          | free       | small group use         | not meant for production use, but may be sufficient
On-Prem Starter  | postgres (or other) store | "free"     | large/many group use    | requires an upgrade to Pro to get any new updates after 1 year
On-Prem Pro      | postgres (or other) store | $5000/yr   | large/many group use    | same as Starter, but, with support plan and continous updates
Cloud Solo       | AWS Dynamo DB             | $30/mth    | small group use         | running on AWS via AWS Marketplace, limited scalability
Cloud Pro        | AWS Dynamo DB             | $30/mth+   | large/many group use    | running on AWS via AWS Marketplace, "fully scalable"

The instructions below will continue with the `Datomic On-Prem Free` option.

1. On your server, download Datomic Free: https://my.datomic.com/downloads/free
   (download the version that corresponds to the current version in Braid's `project.clj`; as of 2020-04-15: `0.9.5697`)
2. Unzip it: `unzip datomic-free-0.9.5703.zip`
3. `cd datomic-free-0.9.5703`
4. Start Datomic: `bin/transactor config/samples/free-transactor-template.properties`

(to run in the background, you could do: `nohup config/samples/free-transactor-template.properties &`

(in the future, you may want to run this with something like supervisor)


## Running Braid

1. Navigate to the directory with the Braid uberjar file.
2. Create a file as follows, changing the variables as you wish:
```
#!/bin/bash

HTTP_PORT=5000
REPL_PORT=6000
export HMAC_SECRET="replace-me-with-randomly-generated-secret"
export ENVIRONMENT=prod
export HTTP_ONLY="true"
export DB_URL="datomic:free://localhost:4334/braid"

java -server -Xmx1228m -Dfile.encoding=UTF8 \
     -Dclojure.server.repl="{:port $REPL_PORT :accept clojure.core.server/repl}" \
     -jar braid-0.0.1-standalone.jar $HTTP_PORT
```
3. `chmod 755 start.sh`
4. `./start.sh`

The above will start three servers:
  1. a desktop client server (port 5000)
  2. a mobile client server (port 5001) (currently very WIP)
  3. an api server (port 5002)

You should be able to visit `http://localhost:5000` (or `http://your-server-ip:5000`) and see the Braid desktop client.

If you ever need to, you can REPL into the running application (!!!): `rlwrap telnet localhost 6000`

## Creating Your First User and Group

1. Navigate to `http://localhost:5000`, you should see a Login Form.
2. Click 'Register' at the bottom.
3. Enter an email and password, then click 'Create a Braid Account'
4. You should then see the option to 'Create Group', click that.
5. You should see a form 'Start a New Braid Group', enter a name for the group, a URL, and a group type (reccomended: private)
6. Submit the form. You will then be redirected to your group.

## Inviting Other Users

1. Hover over your name in the top right
2. Select 'Invite a Person'
3. Click Generate and then copy the invite link
4. Send the invite link to other users (via some other communication method)

(If you have email configured, you can use the Invite by Email)


You are now ready to try out Braid!

<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>

## (Optional) Configure Various External APIs

`profiles.samples.clj` lists various other ENV variables you can set to enable certain features, such as email for password-recovery and digests (via mailgun), file uploads (via AWS S3), website auto-embeds (via embedly), and github-login.

## Configuring S3

Create a bucket (for this example 'braid-bucket'), with the following CORS:

```
<?xml version="1.0" encoding="UTF-8"?>
<CORSConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">>
 <CORSRule>
   <AllowedOrigin>https://clientorigin.com</AllowedOrigin>
   <AllowedMethod>GET</AllowedMethod>
   <AllowedMethod>POST</AllowedMethod>
 </CORSRule>
</CORSConfiguration>
```


Create an IAM user with the following policy:
```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:ListBucket",
                "s3:DeleteObject"
            ],
            "Resource": [
                "arn:aws:s3:::braid-bucket",
                "arn:aws:s3:::braid-bucket/*"
            ]
        }
    ]
}
```

In profiles / ENV, provide the following keys (sample values provided below):
```
  :aws-bucket "braid-bucket"
  :aws-region "us-east-1"
  :aws-access-key "ABC123..."
  :aws-secret-key "1234..."
```

## (Optional) Reverse Proxy with Nginx

To use Braid with a custom domain and with HTTPS, the recommended strategy is to reverse proxy the Braid servers with Nginx and use LetsEncrypt.

If you are using a custom domain, you should set the SITE_URL and API_DOMAIN variables
  (ex. `SITE_URL="https://braid.example.com"` , `API_DOMAIN="braid-api.example.com"`)


## (Optional) Adding Redis

Redis can be used for storing http sessions and invite tokens.
Without Redis, every time you restart the server, all users will have to re-log-in and any active invite tokens will be lost.

You can enable use of Redis in Braid by passing in a `REDIS_URI` via ENV, ex.  `redis://127.0.0.1:6379`


## (Optional) Using Datomic Pro

To run Datomic Pro locally, you will need to:

  - sign up for an account with Datomic
  - download the pro version
  - unzip and run the transactor similarly as with the free version (but different template file)
  - add the following to the lein profile (in your profiles.clj):

```clojure
  {:custom
    [:prod
     :datomic-pro
      {:env {:db-url "datomic:dev://localhost:4333/braid"}}
       :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                        :creds :gpg
                                        :username "USERNAME-HERE"
                                        :password "PASSWORD-HERE"}}}]}
```

Datomic supports several backing stores. We like using Postgres, but, others should work just as well.

You will need to set the backing store via the DB_URL ENV variable:
```
export DB_URL: "datomic:sql://some_datomic_db_name?jdbc:postgresql://localhost:5432/some_postgres_db_name?user=someuser&password=somepassword"
```

To build an Uberjar with this profile, you can run: `lein with-profile +custom uberjar` and follow instructions as above.

To run locally with leiningen, you can do: `lein with-profile +custom repl`

## Hosted Version of Braid

If you want want to use Braid but avoid the hassle of deploying and maintaining a server, the creators of Braid offer a hosted version: http://www.braidchat.com


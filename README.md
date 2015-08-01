# starting

```bash
# start datomic db elsewhere
lein with-profile lp repl
```

```clojure
(chat.server.db/init!) ; first time only

(chat.server.seed/seed!) ; optional

(chat.server.handler/start-server! 5555)
(chat.server.sync/start-router!)
```

# running tests

`lein with-profile test,lp quickie "chat.*"`


# deploying

`./deploy.sh`

then ssh to chat.leanpixel.com and

`sudo supervisorctl restart chat`


# production notes

nrepl is started on port 3081.  To connect, forward the port when ssh'ing in, e.g.

`ssh -L 3081:localhost:3081 chat.leanpixel.com`

then you can `lein repl :connect 3081` locally.

App log files are at `/www/deploys/chat/chat-server.log` and `/www/deploys/chat/chat-server.err.log` (stdout & stderr, respectively)

Datomic logs are at `/home/james/datomic.log` and `/home/james/datomic.err.log` (bad location, I should update this)

Nginx logs are at `/var/log/nginx/chat.access.log` and `/var/log/nginx/chat.error.log`


# initial remote setup

setting up a production instance of chat looks as follows:

(assuming a server has been deployed, users & ssh keys added for us, root login disabled, users added to admin group, etc)

## install required programs:

`sudo apt-get install postgresql nginx redis-server supervisor`

[Download & install Java 1.8 from Oracle](http://www.devsniper.com/install-jdk-8-on-ubuntu/)

Download datomic (following instructions on my.datomic.com), unzip it somewhere

## configuration stuff

### postgresql & datomic

Allow datomic user access to the postgres database by adding the following line to `/etc/postgresql/9.3/main/pg_hba.conf` *above* the line beginning with `local all all`

    local   datomic         all                                     trust

and restarting postgres.

create the datomic postgresql database

```bash
# create the datomic user & set password
sudo -u postgres createuser datomic
sudo -u postgres psql -c "alter user datomic with unencrypted password 'datomic';"
# create the datomic db
sudo -u postgres createdb -E 'UTF8' -T template0 --lc-collate 'en_US.UTF-8' --lc-ctype 'en_US.UTF-8' -O datomic datomic
# set up the datomic schema
psql -U datomic datomic <<EOF
CREATE TABLE datomic_kvs
(
 id text NOT NULL,
 rev integer,
 map text,
 val bytea,
 CONSTRAINT pk_id PRIMARY KEY (id )
)
WITH (
 OIDS=FALSE
);
EOF
```

In the extracted datomic directory, copy the `sql-transactor-template.properties` file to `config/sql-transactor.properties` and add the license key in the appropriate place.

create `/etc/supervisor/conf.d/datomic.conf` (or chat.conf, if you want to have datomic & chat configs in the same file, shouldn't make a difference), with contents that should look something like the following:

```
[program:datomic]
command=/home/james/datomic-pro-0.9.5201/bin/transactor /home/james/datomic-pro-0.9.5201/config/sql-transactor.properties
autostart=true
autorestart=true
startsecs=10
stdout_logfile=/home/james/datomic.log
stderr_logfile=/home/james/datomic.err.log
directory=/home/james/datomic-pro-0.9.5201/
startretries=3
```

(datomic should probably live somewhere other than my home directory, which would mean changing the command, stdout_logfile, stderr_logfile, and directory options)

you can now start & stop datomic via `supervisorctl` and it should automatically start

## set up deploy directory

create a "deploy" group (`sudo groupadd deploy`) and add relevant users to the group (`sudo usermod -a -G deploy $user`)

to make the directory writeable by users in the `deploy` group, it's easiest to use ACLs.  To do so, first set the mount flags to enable ACLs, by adding the "acl" option /etc/fstab, which should look something like this:

    UUID=050e1e34-39e6-4072-a03e-ae0bf90ba13a /               ext4    errors=remount-ro,acl 0       1

to remount with the acl option without rebooting, run `sudo mount -o remount,acl /`


Next, install the acl tools (`sudo apt-get install acl`) and set the permissions on the deploy directory; something like this:

```bash
sudo mkdir -p /www/deploys
setfacl -d -m group:deploy:rwx /www/deploys
setfacl -m group:deploy:rwx /www/deploys
```

Now all users in the `deploy` group can create new files & directories under the /www/deploys dir and will have the same permissions applied to the new files.

## set up application and supervisord

create a deploy directory (e.g. `/www/deploys/chat`), upload an uberjar, and (for ease of reloading), create a symlink to the uploaded jar (e.g. so `chat.jar` points to `chat-2015-07-27_125926.jar`)

add a supervisor entry for the chat app, something like this:

```
[program:chat]
command=java -server -Xmx1g -jar /www/deploys/chat/chat.jar 5555 3081
environment=ENVIRONMENT="prod",TESTER_PASSWORD="test user password",DB_URL="datomic:sql://chat_prod?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic"
autostart=true
autorestart=true
startsecs=10
stdout_logfile=/www/deploys/chat/chat-server.log
stderr_logfile=/www/deploys/chat/chat-server.err.log
directory=/www/deploys/chat
startretries=3
```

When seeding, you may want to include RAFAL_PASSWORD and JAMES_PASSWORD in the environment for the first time you run seed, or just manually connect a repl and create it by hand instead of muddling things.

## setting up nginx

assuming the appropriate DNS entries are pointing to the server

upload the SSL .crt and .key files to /etc/nginx/certs

create an ngnix config in sites-available looking something like this:

```
upstream chat_backend {
  server 127.0.0.1:5555;
  keepalive 32;
}

server {
  listen 80;
  server_name chat.leanpixel.com;
  return 301 https://chat.leanpixel.com$request_uri;
}

# need this for websocket connection upgrade
map $http_upgrade $connection_upgrade {
  default upgrade;
  ''      close;
}

server {
  listen 443;
  server_name chat.leanpixel.com;

  ssl on;
  ssl_certificate /etc/nginx/certs/leanpixel.com.crt;
  ssl_certificate_key /etc/nginx/certs/leanpixel.com.key;
  ssl_session_timeout 5m;
  ssl_session_cache shared:CHAT-SSL:10m;
  ssl_prefer_server_ciphers on;
  ssl_protocols SSLv3 TLSv1 TLSv1.1 TLSv1.2;
  ssl_ciphers "EECDH+ECDSA+AESGCM EECDH+aRSA+AESGCM EECDH+ECDSA+SHA384 EECDH+ECDSA+SHA256 EECDH+aRSA+SHA384 EECDH+aRSA+SHA2    56 EECDH+aRSA+RC4 EECDH EDH+aRSA RC4 !aNULL !eNULL !LOW !3DES !MD5 !EXP !PSK !SRP !DSS";

  access_log /var/log/nginx/chat.access.log;
  error_log /var/log/nginx/chat.error.log;

  location / {
    proxy_pass http://chat_backend;
    proxy_redirect off;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header        Host            $host;
    proxy_set_header        X-Real-IP       $remote_addr;
    proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
    # set upgrade headers for websockets
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
  }

}
```

then symlink it into sites-enabled, delete the symlink to `default` in sites-enabled and `sudo service nginx reload`

## Annoying stuff

May need to also `sudo apt-get install haveged` to make sure there's enough
entropy to hash passwords when creating users

Things should be working now!

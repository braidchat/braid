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

## Security stuff

Disable root login and disallow cleartext logins over ssh

```
sudo apt-get install fail2ban
sudo service fail2ban start
```

```
sudo ufw allow ssh
sudo ufw allow www
sudo ufw allow 443
sudo ufw enable
```

## install required programs:

`sudo apt-get install postgresql nginx redis-server supervisor fail2ban`

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
command=java -server -Xmx1g -Dfile.encoding=UTF8 -jar /www/deploys/chat/chat.jar 5555 3081
environment=ENVIRONMENT="prod",TESTER_PASSWORD="test user password",DB_URL="datomic:sql://chat_prod?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic",TIMBRE_LEVER=":debug",MALIGUN_PASSWORD="XXX",MAILGUN_DOMAIN="chat.leanpixel.com",AWS_DOMAIN="chat.leanpixel.com",AWS_ACCESS_KEY="XXX",AWS_SECRET_KEY="...",S3_UPLOAD_KEY="XXX",S#_UPLOAD_SECRET="XXX",ASANA_CLIENT_ID="XXX",ASANA_CLIENT_SECRET="XXX",API_DOMAIN="api.mydomain.com",GITHUB_CLIENT_ID="XXX",GITHUB_CLIENT_ID="XXX"
autostart=true
autorestart=true
startsecs=10
stdout_logfile=/www/deploys/chat/chat-server.log
stderr_logfile=/www/deploys/chat/chat-server.err.log
directory=/www/deploys/chat
startretries=3
```

When seeding, you may want to include RAFAL_PASSWORD and JAMES_PASSWORD in the environment for the first time you run seed, or just manually connect a repl and create it by hand instead of muddling things.

## Webserver

assuming the appropriate DNS entries are pointing to the server (the example
below assumes the main site is `braid.chat`, mobile is `m.braid.chat`, api is
`api.braid.chat` and `www.braid.chat` redirects to `braid.chat`.

### Configuring nginx

create an ngnix config in sites-available looking something like this:

```
upstream braid_desktop {
  server 127.0.0.1:5555;
  keepalive 32;
}

upstream braid_mobile {
  server 127.0.0.1:5556;
  keepalive 32;
}

upstream braid_api {
  server 127.0.0.1:5557;
  keepalive 32;
}


map $http_upgrade $connection_upgrade {
  default upgrade;
  ''      close;
}

## braid.chat

# http redirects
server {
  listen 80;
  server_name api.braid.chat m.braid.chat braid.chat;
  # for letsencrypt verification
  location /.well-known {
    default_type "text/plain";
    root /usr/share/nginx/html;
  }
  location / {
    return 301 https://$host$request_uri;
  }
}
server {
  listen 80;
  server_name www.braid.chat;
  # for letsencrypt verification
  location /.well-known {
    default_type "text/plain";
    root /usr/share/nginx/html;
  }
  location / {
    return 301 https://braid.chat$request_uri;
  }
}

# redirect www to bare domain
server {
  listen 443 ssl;
  listen [::]:443 ssl;
  server_name www.braid.chat;

  ssl on;
  ssl_certificate /etc/letsencrypt/live/braid.chat/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/braid.chat/privkey.pem;
  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
  ssl_prefer_server_ciphers on;
  ssl_dhparam /etc/ssl/certs/dhparam.pem;
  ssl_ciphers 'ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES256-SHA:ECDHE-ECDSA-DES-CBC3-SHA:ECDHE-RSA-DES-CBC3-SHA:EDH-RSA-DES-CBC3-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:DES-CBC3-SHA:!DSS';
  ssl_session_timeout 1d;
  ssl_session_cache shared:BRAID-SSL:50m;
  ssl_stapling on;
  ssl_stapling_verify on;
  ssl_trusted_certificate /etc/letsencrypt/live/braid.chat/chain.pem;
  add_header Strict-Transport-Security max-age=15768000;

  return 301 https://braid.chat$request_uri;
}

# bare domain/desktop site
server {
  listen 443 ssl;
  listen [::]:443 ssl;
  server_name braid.chat;

  ssl on;
  ssl_certificate /etc/letsencrypt/live/braid.chat/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/braid.chat/privkey.pem;
  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
  ssl_prefer_server_ciphers on;
  ssl_dhparam /etc/ssl/certs/dhparam.pem;
  ssl_ciphers 'ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES256-SHA:ECDHE-ECDSA-DES-CBC3-SHA:ECDHE-RSA-DES-CBC3-SHA:EDH-RSA-DES-CBC3-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:DES-CBC3-SHA:!DSS';
  ssl_session_timeout 1d;
  ssl_session_cache shared:BRAID-SSL:50m;
  ssl_stapling on;
  ssl_stapling_verify on;
  ssl_trusted_certificate /etc/letsencrypt/live/braid.chat/chain.pem;
  add_header Strict-Transport-Security max-age=15768000;

  access_log /var/log/nginx/chat.access.log;
  error_log /var/log/nginx/chat.error.log;

  location / {
    proxy_pass http://braid_desktop;
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

# mobile site
server {
  listen 443 ssl;
  listen [::]:443 ssl;

  server_name m.braid.chat;

  ssl on;
  ssl_certificate /etc/letsencrypt/live/braid.chat/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/braid.chat/privkey.pem;
  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
  ssl_prefer_server_ciphers on;
  ssl_dhparam /etc/ssl/certs/dhparam.pem;
  ssl_ciphers 'ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES256-SHA:ECDHE-ECDSA-DES-CBC3-SHA:ECDHE-RSA-DES-CBC3-SHA:EDH-RSA-DES-CBC3-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:DES-CBC3-SHA:!DSS';
  ssl_session_timeout 1d;
  ssl_session_cache shared:BRAID-SSL:50m;
  ssl_stapling on;
  ssl_stapling_verify on;
  ssl_trusted_certificate /etc/letsencrypt/live/braid.chat/chain.pem;
  add_header Strict-Transport-Security max-age=15768000;

  access_log /var/log/nginx/chat.access.log;
  error_log /var/log/nginx/chat.error.log;

  location / {
    proxy_pass http://braid_mobile;
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

# api backend
server {
  listen 443 ssl;
  listen [::]:443 ssl;

  server_name api.braid.chat;

  ssl on;
  ssl_certificate /etc/letsencrypt/live/braid.chat/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/braid.chat/privkey.pem;
  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
  ssl_prefer_server_ciphers on;
  ssl_dhparam /etc/ssl/certs/dhparam.pem;
  ssl_ciphers 'ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES256-SHA:ECDHE-ECDSA-DES-CBC3-SHA:ECDHE-RSA-DES-CBC3-SHA:EDH-RSA-DES-CBC3-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:DES-CBC3-SHA:!DSS';
  ssl_session_timeout 1d;
  ssl_session_cache shared:BRAID-SSL:50m;
  ssl_stapling on;
  ssl_stapling_verify on;
  ssl_trusted_certificate /etc/letsencrypt/live/braid.chat/chain.pem;
  add_header Strict-Transport-Security max-age=15768000;

  access_log /var/log/nginx/chat.access.log;
  error_log /var/log/nginx/chat.error.log;

  location / {
    proxy_pass http://braid_api;
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

### SSL

Using [letsencrypt](https://letsencrypt.org/):

Install letsencrypt on the server as instructed on the site to `/opt/`

Generate the certificate:

```bash
$ /opt/letsencrypt/letsencrypt-auto certonly -a webroot --webroot-path=/usr/share/nginx/html -d braid.chat -d www.braid -d m.braid.chat -d api.braid.chat
```

Generate a strong Diffe-Hellman group - we want to avoid using comment groups
that attackers may have precomputed.

```bash
$ sudo openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048
```

Set up a cron job to autorenew the certificate (letsencrypt certs only last for
60 days).

Edit the root cron with `sudo crontab -e` and add the following lines:

```
30 2 * * 1 /opt/letsencrypt/letsencrypt-auto renew >> /var/log/le-renew.log
35 2 * * 1 /usr/sbin/service nginx reload
```

## backups

Backing up the underlying PostgreSQL store:

Create a pgbackup user with the appropriate permissions:

```
$ sudo useradd pgbackup -m
$ sudo -u postgres createuser pgbackup
$ sudo -u postgres psql datomic -c 'GRANT SELECT ON ALL TABLES IN SCHEMA public TO pgbackup;'
$ sudo -u postgres psql datomic -c 'GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO pgbackup;'
```

Install python and the `boto` library to run the backup script.
On Ubuntu, you can install with apt: `sudo apt-get install python-boto`.
If your server doesn't have an OS package, you can install pip and use pip to install boto.

Upload `glacier_backup.py` to the pgbackup user's home directory.
Create access keys in the AWS management console Glacier and update the credentials in `glacier_backup.py`.
If using the Amazon-recommended method of creating a limited user account, be sure to give the account at least Glacier write permissions.
Create a Glacier vault and update the vault name and region name variables in `glacier_backup.py`.

Create a file in `/etc/cron.daily/datomic_backup` with the following content:

```
#!/bin/bash

set -euo pipefail
backup_file="/tmp/datomic_backup_$(date -Isec).sql.gz"
sudo -u pgbackup bash -c "pg_dump datomic | gzip > \"${backup_file}\""
sudo -u pgbackup ~pgbackup/glacier_backup.py "${backup_file}"
rm "${backup_file}"
```

(Note that if using Ubuntu, the file name cannot contain any period characters or it won't be run by cron).

Be sure to make the file executable `sudo chmod +x /etc/cron.daily/datomic_backup`.

## Annoying stuff

May need to also `sudo apt-get install haveged` to make sure there's enough
entropy to hash passwords when creating users

Things should be working now!

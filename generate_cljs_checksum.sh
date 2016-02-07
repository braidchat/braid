#!/bin/sh

CHECKSUM_FILE="src/chat/shared/checksum.cljc"
CLIENT_SUM=$(find "resources/less" "src/chat/client" -type f -exec md5 -q {} + | sort | md5 -q)
cat > $CHECKSUM_FILE <<EOM
(ns chat.shared.checksum)

(def current-client-checksum "$CLIENT_SUM")
EOM

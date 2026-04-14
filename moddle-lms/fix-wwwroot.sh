#!/bin/bash
# fix-wwwroot.sh — Runs after Bitnami finishes Moodle init.
# Fixes the wwwroot PHP concatenation bug that Bitnami sometimes generates.
#
# Bitnami bug:  $CFG->wwwroot = 'http://IP:8080' . 'IP';  ← invalid URL
# Expected:     $CFG->wwwroot = 'http://IP:8080';

CONFIG="/bitnami/moodle/config.php"

if [ ! -f "$CONFIG" ]; then
  echo "[fix-wwwroot] config.php not found yet — skipping"
  exit 0
fi

# Only fix if the concatenation bug is present (contains ". '")
if grep -q "\. '" "$CONFIG" 2>/dev/null; then
  # Extract the correct URL from the first quoted string on the wwwroot line
  CURRENT=$(grep 'wwwroot' "$CONFIG" | head -1 | sed "s/.*= '\\([^']*\\)'.*/\\1/")
  if [ -n "$CURRENT" ]; then
    sed -i "s|\$CFG->wwwroot.*|\$CFG->wwwroot   = '${CURRENT}';|" "$CONFIG"
    echo "[fix-wwwroot] Fixed wwwroot to: $CURRENT"
  fi
else
  echo "[fix-wwwroot] wwwroot looks OK — no fix needed"
fi

# Ensure auth_userkey plugin is in place (volume may overwrite /bitnami/moodle)
if [ ! -d "/bitnami/moodle/auth/userkey" ] && [ -d "/opt/bitnami/moodle/auth/userkey" ]; then
  cp -r /opt/bitnami/moodle/auth/userkey /bitnami/moodle/auth/userkey
  chown -R 1001:root /bitnami/moodle/auth/userkey
  echo "[fix-wwwroot] Copied auth_userkey plugin into volume"
fi

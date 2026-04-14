# ============================================================
#  Custom Moodle image – bakes in auth_userkey plugin
#  so no manual install is needed after deployment.
# ============================================================
FROM bitnamilegacy/moodle:4.5

USER root

# Install tools needed to download & extract the plugin
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends curl unzip && \
    rm -rf /var/lib/apt/lists/*

# Download auth_userkey plugin and place it in the auth directory
RUN cd /tmp && \
    curl -L -o userkey.zip \
      "https://github.com/catalyst/moodle-auth_userkey/archive/refs/heads/MOODLE_33PLUS.zip" && \
    unzip -q userkey.zip && \
    mkdir -p /opt/bitnami/moodle/auth/userkey && \
    cp -r moodle-auth_userkey-MOODLE_33PLUS/. /opt/bitnami/moodle/auth/userkey/ && \
    chown -R daemon:daemon /opt/bitnami/moodle/auth/userkey && \
    rm -rf userkey.zip moodle-auth_userkey-MOODLE_33PLUS

# Copy the entrypoint helper that fixes wwwroot after Bitnami init
COPY fix-wwwroot.sh /docker-entrypoint-initdb.d/fix-wwwroot.sh
RUN chmod +x /docker-entrypoint-initdb.d/fix-wwwroot.sh

USER 1001

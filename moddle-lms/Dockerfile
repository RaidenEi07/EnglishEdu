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
# Bitnami runs as UID 1001 — files MUST be owned by 1001:root
RUN cd /tmp && \
    curl -L -o userkey.zip \
      "https://github.com/catalyst/moodle-auth_userkey/archive/refs/heads/MOODLE_33PLUS.zip" && \
    unzip -q userkey.zip && \
    mkdir -p /opt/bitnami/moodle/auth/userkey && \
    cp -r moodle-auth_userkey-MOODLE_33PLUS/. /opt/bitnami/moodle/auth/userkey/ && \
    chown -R 1001:root /opt/bitnami/moodle/auth/userkey && \
    chmod -R g+rwX /opt/bitnami/moodle/auth/userkey && \
    rm -rf userkey.zip moodle-auth_userkey-MOODLE_33PLUS && \
    \
    curl -L -o moove.zip \
      "https://github.com/willianmano/moodle-theme_moove/archive/refs/heads/master.zip" && \
    unzip -q moove.zip && \
    mkdir -p /opt/bitnami/moodle/theme/moove && \
    cp -r moodle-theme_moove-master/. /opt/bitnami/moodle/theme/moove/ && \
    chown -R 1001:root /opt/bitnami/moodle/theme/moove && \
    chmod -R g+rwX /opt/bitnami/moodle/theme/moove && \
    rm -rf moove.zip moodle-theme_moove-master

# Copy the entrypoint helper that fixes wwwroot after Bitnami init
COPY fix-wwwroot.sh /docker-entrypoint-initdb.d/fix-wwwroot.sh
RUN chmod +x /docker-entrypoint-initdb.d/fix-wwwroot.sh

# Copy custom question types
COPY custom_plugins/qtype_sectionaudio /opt/bitnami/moodle/question/type/sectionaudio
COPY custom_plugins/qtype_splitreading /opt/bitnami/moodle/question/type/splitreading
RUN chown -R 1001:root /opt/bitnami/moodle/question/type/sectionaudio /opt/bitnami/moodle/question/type/splitreading && \
    chmod -R g+rwX /opt/bitnami/moodle/question/type/sectionaudio /opt/bitnami/moodle/question/type/splitreading

USER 1001

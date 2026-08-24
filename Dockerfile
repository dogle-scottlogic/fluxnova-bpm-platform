# Fluxnova BPM Platform (Spring Boot / "Run") container image.
#
# Build prerequisite:
#   Build and package the project locally first, e.g.:
#     mvn clean install -DskipTests -DskipITs
#   This produces distro/run/distro/target/fluxnova-bpm-run-*.zip, which this
#   image unpacks and runs. The Docker build itself does not compile the project.
#
# Runtime configuration overrides (no image rebuild required):
#   The engine configuration lives at /fluxnova/configuration inside the image.
#   Mount a replacement file or directory over it at "docker run" time, e.g.:
#     docker run -v "$(pwd)/my-default.yml:/fluxnova/configuration/default.yml:ro" ...
#   or to fully replace the configuration directory (e.g. to add extra plugin jars
#   to configuration/userlib):
#     docker run -v "$(pwd)/my-config:/fluxnova/configuration:ro" ...
#   Database connection settings can also be supplied via environment variables,
#   see docker-script.sh (DB_DRIVER, DB_URL, DB_USERNAME, DB_PASSWORD, ...).

FROM amazoncorretto:21

ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto \
    PATH=$PATH:$JAVA_HOME/bin

WORKDIR /fluxnova

# Install required packages.
# --allowerasing resolves a curl/curl-minimal package conflict on current Amazon Linux 2023.
RUN yum install -y --allowerasing jq curl shadow-utils unzip findutils \
    && yum clean all \
    && rm -rf /var/cache/yum

# Create a dedicated non-root user/group to own and run the application.
RUN groupadd -g 4001 fluxnova_group \
    && adduser -G fluxnova_group -u 4001 -m -d /fluxnova fluxnova_user

# Unpack the pre-built Spring Boot distribution (see build prerequisite above).
COPY distro/run/distro/target/fluxnova-bpm-run-*.zip /fluxnova/fluxnova-bpm-run.zip
RUN unzip fluxnova-bpm-run.zip \
    && rm fluxnova-bpm-run.zip

COPY docker-script.sh /fluxnova/docker-script.sh

# Normalize line endings (in case scripts were built/checked out with CRLF, e.g. on Windows),
# make scripts executable, and hand ownership of the application directory to the runtime user.
RUN find /fluxnova -name "*.sh" -exec sed -i 's/\r$//' {} + \
    && chmod +x /fluxnova/*.sh /fluxnova/internal/*.sh \
    && chown -R fluxnova_user:fluxnova_group /fluxnova

# Allows configuration to be swapped out at runtime via a bind mount without rebuilding the image.
VOLUME ["/fluxnova/configuration"]

USER 4001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fs http://localhost:8080/engine-rest/engine > /dev/null || exit 1

ENTRYPOINT ["sh", "docker-script.sh"]

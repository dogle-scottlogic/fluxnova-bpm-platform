# Fluxnova BPM Platform Build Guide

This guide contains the following main sections:

1. **Building Locally:**  
   Step-by-step instructions to build `fluxnova-bpm-platform` on your local machine.

2. **Building with GitHub Actions on FINOS:**  
   Details on how the project is built, tested, and deployed automatically using GitHub Actions, including workflow triggers and package management.

3. **Running Fluxnova BPM Platform:**  
   Instructions for running Fluxnova in Tomcat and Spring Boot modes after building.

## Contents

- [Building Fluxnova BPM Platform Locally](#building-fluxnova-bpm-platform-locally)
- [Building with GitHub Actions on FINOS](#building-with-github-actions-on-finos)
- [Running Fluxnova BPM Platform](#running-fluxnova-bpm-platform)
    - [Running the Tomcat Version](#running-the-tomcat-version)
    - [Running the Spring Boot Version](#running-the-spring-boot-version)
    - [Running with Docker](#running-with-docker)

Please follow the relevant section based on your build environment.

## Building Fluxnova BPM Platform Locally

### Build Prerequisites

- Java 21+
- Maven 3.8+
- Git

### Build Steps

All Fluxnova dependencies are now public and published to Maven Central.

1. **Clone and Build `fluxnova-bpm-platform` Repository**

   ```bash
   git clone https://github.com/finos/fluxnova-bpm-platform
   cd fluxnova-bpm-platform
   mvn clean install
   ```

   - To skip tests:
     ```bash
     mvn clean install -DskipTests -DskipITs
     ```
   - To run all tests:
     ```bash
     mvn clean install
     ```

### Notes

- All dependencies are resolved from Maven Central.
- Artifacts from each build will be stored in your local Maven repository and used by subsequent builds.

### Building the Agentic Subprocess Plugins from the `fluxnova-plugins` Fork

The agentic subprocess engine plugins (`fluxnova-engine-plugins-ai-agent-*`) are consumed as `1.0.0-SNAPSHOT`
dependencies. Because that version is a plain (non-timestamped) SNAPSHOT coordinate, it could in principle be
resolved from *any* build published to the public Sonatype OSSRH snapshots repository under that coordinate — not
necessarily one built from the [dogle-scottlogic/fluxnova-plugins](https://github.com/dogle-scottlogic/fluxnova-plugins)
fork. `mvn -U` (force update) in particular can silently overwrite a locally built copy with whatever happens to be
published remotely.

The two repositories also depend on each other: this repo's `engine` module depends on
`fluxnova-engine-plugins-ai-agentic-subprocess`, while the plugins depend on `fluxnova-engine-plugin-otel`, which is
built only here. To guarantee the plugins are built from the fork, and in the right order, use the provided helper
script instead of building `fluxnova-bpm-platform` directly:

```bash
./build-with-plugins.sh clean install -DskipTests -DskipITs
```

It expects the plugins fork checked out next to this repository (`../fluxnova-plugins`) and builds in three stages:

1. Installs this repo's parent POMs and the OTEL engine plugin (compiled against the released `fluxnova-engine` of the
   same version from Maven Central).
2. Installs the agentic subprocess plugins from `../fluxnova-plugins/agentic-subprocess/pom.xml`, **with** their tests
   unless `FLUXNOVA_PLUGINS_SKIP_TESTS=true`. (The `fluxnova-plugins` root POM has no modules, so building it alone
   installs only the parent POM.)
3. Builds `fluxnova-bpm-platform` with the arguments you pass, **without** `-U`, so Maven reuses the freshly installed
   local jars rather than checking the network.

An existing plugins checkout is built as-is, including local unpushed commits; it is never reset or switched. If it
is missing, the script clones branch `upgrade-3.1.1` (configurable via `FLUXNOVA_PLUGINS_BRANCH`). See the comments at
the top of `build-with-plugins.sh` for the other options.

**Windows:** clone this repository with `core.autocrlf=false` (for example
`git clone -c core.autocrlf=false -c core.longpaths=true ...`). With `autocrlf=true` the webapp's Prettier lint
rejects the CRLF line endings and `npm run build` fails in `fluxnova-webapp-root`.

-----------------------------------------------------------------------------------------------------------------------

## Building with GitHub Actions on FINOS

The `fluxnova-bpm-platform` project uses GitHub Actions for automated CI/CD on FINOS. This workflow builds, tests, and deploys the project.

### Workflow Triggers

- **Automatic Triggers:**  
  The workflow runs automatically on every push to the `main` branch and on every pull request.
- **Manual Trigger:**  
  You can manually trigger the workflow from the GitHub Actions tab using the "Run workflow" button.  
  When using this manual trigger (`workflow_dispatch`), you can select which branch to build.

### Steps in the Workflow

1. **Checkout Repository:**  
   Checks out the latest code from the selected branch.
2. **Java Setup:**  
   Sets up the required Java version.
3. **Git Configuration:**  
   Configures Git for the workflow environment.
4. **Set Project Version:**  
   Sets the Maven project version.
5. **Download Dependencies:**  
   Resolves and downloads all Maven dependencies and plugins.
6. **Build and Deploy:**  
   Builds the project and deploys artifacts to maven central.
7. **Run Tests:**  
   Runs unit and integration tests in separate jobs.

## Running Fluxnova BPM Platform

You can run Fluxnova in two modes: **Tomcat** and **Spring Boot**.

### Running the Tomcat Version

1. After a successful build, extract the Tomcat distribution archive into a directory **outside** the repository.
   The archive has no top-level folder — extracting it into the repository root will clutter your `git status` with untracked distribution files. The `-d` target below (`~/fluxnova-tomcat`) is just an example; change it to any location you prefer:
   ```bash
   unzip distro/tomcat/distro/target/fluxnova-bpm-tomcat-0.0.1-SNAPSHOT.zip -d ~/fluxnova-tomcat
   ```
2. Navigate to the extraction directory:
   ```bash
   cd ~/fluxnova-tomcat
   ```
3. Start the Fluxnova Tomcat server:
   ```bash
   sh start-fluxnova.sh
   ```
4. Access the Fluxnova Monitoring interface at [http://localhost:8080/fluxnova/app/monitoring/default/#/dashboard](http://localhost:8080/camunda/app/cockpit/default/#/dashboard)

### Running the Spring Boot Version

1. After a successful build, extract the Spring Boot distribution archive into a directory **outside** the repository.
   The archive has no top-level folder — extracting it into the repository root will clutter your `git status` with untracked distribution files. The `-d` target below (`~/fluxnova-run`) is just an example; change it to any location you prefer:
   ```bash
   unzip distro/run/distro/target/fluxnova-bpm-run-0.0.1-SNAPSHOT.zip -d ~/fluxnova-run
   ```
2. Navigate to the extraction directory:
   ```bash
   cd ~/fluxnova-run
   ```
3. Start the Spring Boot server:
   ```bash
   sh start.sh
   ```
4. Access the Fluxnova Monitoring interface at [http://localhost:8080/fluxnova/app/monitoring/default/#/dashboard](http://localhost:8080/fluxnova/app/cockpit/default/#/dashboard)

### Running with Docker

You can run the Fluxnova BPM Platform using the official Docker image. By default, the image uses an embedded H2 database. You can customize it to use PostgreSQL or MySQL by providing the required JDBC driver version and database connection details.

#### Pull the Docker Image

```bash
docker pull ghcr.io/finos/fluxnova-bpm-platform:1.1.0
```

#### Run the Container (Default: H2 Database)

```bash
docker run -p 8080:8080 ghcr.io/finos/fluxnova-bpm-platform:1.1.0
```

#### Run with PostgreSQL or MySQL

To use PostgreSQL or MySQL, pass the driver version as a build argument and provide DB connection variables as environment variables:

```bash
docker run -p 8080:8080 \
  -e DB_DRIVER_CLASS_NAME=org.postgresql.Driver \
  -e DB_URL=jdbc:postgresql://<host>:<port>/<db> \
  -e DB_USERNAME=<username> \
  -e DB_PASSWORD=<password> \
  ghcr.io/finos/fluxnova-bpm-platform:1.1.0
```

Replace the values as needed for MySQL. Any other Spring Boot configuration can be passed as environment variables.

#### Building the Docker Image Locally

The repository root `Dockerfile` builds the Spring Boot image from your own local build output, so you can produce and
run a custom image without waiting on the official published image.

1. Build and package the project (skipping tests for a faster local build):
   ```bash
   mvn clean install -DskipTests -DskipITs
   ```
   This produces `distro/run/distro/target/fluxnova-bpm-run-*.zip`, which the `Dockerfile` unpacks.

   To guarantee the agentic subprocess plugins are built from the `dogle-scottlogic/fluxnova-plugins` fork rather
   than a same-versioned SNAPSHOT from elsewhere, use `./build-with-plugins.sh clean install -DskipTests -DskipITs`
   instead — see
   [Building the Agentic Subprocess Plugins from the `fluxnova-plugins` Fork](#building-the-agentic-subprocess-plugins-from-the-fluxnova-plugins-fork) above.
2. Build the image (the default, last stage is the Alpine image, which includes `curl` for health checks):
   ```bash
   docker build -t fluxnova-bpm-platform:local .
   ```
   The image only needs `Dockerfile`, `docker-script.sh` and the distro zip. With a remote engine such as Podman on a
   VM/WSL, copy those three into a small directory (keeping the `distro/run/distro/target/` path) and build from there,
   rather than uploading the whole checkout as build context.

   **Published `fluxnova-edd-platform` images:** `2.0.0` is Fluxnova 3.1.1 with the fork's plugins, published as a
   multi-arch index (`linux/amd64` + `linux/arm64`) at `ghcr.io/dogle-scottlogic/fluxnova-edd-platform:<version>`.
   Each image carries `org.opencontainers.image.revision` and `org.finos.fluxnova.plugins.revision` labels recording
   the two commits it was built from. The distro zip is pure Java, so both architectures are built from the same zip
   and `Dockerfile`; only the base image differs. On an x86 host, ARM64 builds need QEMU user-mode emulation (e.g.
   `qemu-user-binfmt` on Ubuntu/WSL; check with `podman run --rm --platform linux/arm64 alpine uname -m`). To publish:
   ```bash
   podman build --platform linux/amd64 -t fluxnova-edd-platform:<version>-amd64 <context>
   podman build --platform linux/arm64 -t fluxnova-edd-platform:<version>-arm64 <context>
   podman manifest create fluxnova-edd-platform:<version>
   podman manifest add fluxnova-edd-platform:<version> containers-storage:localhost/fluxnova-edd-platform:<version>-amd64
   podman manifest add fluxnova-edd-platform:<version> containers-storage:localhost/fluxnova-edd-platform:<version>-arm64
   podman manifest push --all fluxnova-edd-platform:<version> docker://ghcr.io/dogle-scottlogic/fluxnova-edd-platform:<version>
   ```
3. Run it (default H2 database, same as the published image):
   ```bash
   docker run -p 8080:8080 fluxnova-bpm-platform:local
   ```

**Overriding configuration without rebuilding the image:** the engine configuration lives at `/fluxnova/configuration`
inside the image. Mount a replacement file or directory over it at `docker run` time to change configuration (e.g.
`default.yml`) or add extra plugin/engine jars to `configuration/userlib` — no rebuild required:

```bash
# Override just the configuration file
docker run -p 8080:8080 \
  -v "$(pwd)/my-default.yml:/fluxnova/configuration/default.yml:ro" \
  fluxnova-bpm-platform:local

# Override the whole configuration directory (e.g. custom default.yml plus extra userlib jars)
docker run -p 8080:8080 \
  -v "$(pwd)/my-config:/fluxnova/configuration:ro" \
  fluxnova-bpm-platform:local
```

> **Note:** the OpenTelemetry process engine plugin's jars (`fluxnova-engine-plugin-otel`, the `io.opentelemetry:*`
> artifacts, `okhttp`/`okio`, and `kotlin-stdlib`) are bundled into `configuration/userlib` as part of the standard
> Maven assembly (see `distro/run/assembly/pom.xml`/`assembly.xml`), so enabling this plugin in your configuration
> works out of the box — no extra `userlib` mount is required.

#### Notes

- The Docker container runs the Spring Boot version of the application and does **not** include additional dependencies like connectors or script engines.
- If you need a Docker image with these dependencies, use the image from the examples repo:

  ```bash
  docker pull ghcr.io/finos/fluxnova-examples:1.0.0-snapshot
  ```
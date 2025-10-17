FROM openjdk:24-slim

# Metadata labels
LABEL name="bitonic-scala3"
LABEL version="1.0.0"
LABEL description="Scala 3 application with ZIO Http and ZIO Redis"


# Install necessary packages for certificate management and curl
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# # Copy the certificate from host machine
COPY cert.pem /usr/local/share/ca-certificates/cert.crt

# # Update the certificate store to include the new certificate
RUN update-ca-certificates

# # Set curl to use the system certificate store
ENV CURL_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt

# Install SBT using the official repository method
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import && \
    chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg && \
    apt-get update && \
    apt-get install -y sbt && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir app

ADD . /app

WORKDIR /app

# Pre-compile to speed up container startup
RUN sbt compile stage

EXPOSE 8080

# Run the application with proper JVM flags for containers
CMD ["sbt", "-J-Xmx512m", "-J-XX:+UseContainerSupport", "run"]

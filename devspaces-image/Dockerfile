FROM quay.io/devfile/universal-developer-image:ubi9-latest

USER 0

RUN dnf install -y --nodocs \
    glib2 \
    nss \
    nspr \
    dbus-libs \
    atk \
    at-spi2-atk \
    cups-libs \
    libxkbcommon \
    at-spi2-core \
    libXcomposite \
    libXdamage \
    libXfixes \
    libXrandr \
    mesa-libgbm \
    pango \
    cairo \
    alsa-lib \
    google-noto-sans-fonts \
    && dnf clean all \
    && rm -rf /var/cache/dnf

USER 10001

RUN npx playwright@1.52.0 install chromium

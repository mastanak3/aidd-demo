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
    vlgothic-fonts \
    postgresql \
    && dnf clean all \
    && rm -rf /var/cache/dnf

USER 10001

RUN bash -lc "sdk default java 21.0.5-tem && sdk default maven 3.9.15"

ENV PLAYWRIGHT_BROWSERS_PATH=/opt/playwright-browsers
ENV npm_config_cache=/tmp/.npm
USER 0
RUN mkdir -p $PLAYWRIGHT_BROWSERS_PATH && chmod 777 $PLAYWRIGHT_BROWSERS_PATH
USER 10001
RUN npx playwright@1.52.0 install chromium

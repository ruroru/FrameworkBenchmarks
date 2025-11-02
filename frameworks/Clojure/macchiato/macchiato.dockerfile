FROM clojure:lein as lein
WORKDIR /macchiato
COPY env env
COPY src src
COPY project.clj project.clj
ENV NODE_ENV=production

# Install Node.js 20
RUN apt-get update -y && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*
RUN lein package

# Stage 2: Runtime stage
FROM node:20-slim
WORKDIR /macchiato
COPY --from=lein /macchiato/target/release/hello.js ./hello.js
COPY --from=lein /macchiato/package.json ./package.json
COPY --from=lein /macchiato/package-lock.json ./package-lock.json
COPY --from=lein /macchiato/node_modules ./node_modules
COPY config.edn config.edn

EXPOSE 3000
CMD ["node", "hello.js"]
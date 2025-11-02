FROM clojure:lein-2.8.1 AS lein
WORKDIR /macchiato
COPY env env
COPY src src
COPY project.clj project.clj
ENV NODE_ENV=production
RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -
RUN apt-get install -yqq nodejs
RUN lein package

# Stage 2: Runtime stage
FROM node:8-slim
WORKDIR /macchiato
COPY --from=builder /macchiato/target/release/hello.js ./hello.js
EXPOSE 3000
CMD ["node", "hello.js"]
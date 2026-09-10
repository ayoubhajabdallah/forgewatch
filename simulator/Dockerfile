FROM golang:1.27.1-alpine AS builder

WORKDIR /app

COPY go.mod ./
COPY *.go ./

RUN CGO_ENABLED=0 GOOS=linux go build \
    -trimpath \
    -ldflags="-s -w" \
    -o /out/forgewatch-simulator .

FROM alpine:3.22

RUN addgroup -S forgewatch \
    && adduser -S -G forgewatch -u 10001 forgewatch

COPY --from=builder /out/forgewatch-simulator /usr/local/bin/forgewatch-simulator

USER forgewatch

ENTRYPOINT ["forgewatch-simulator"]
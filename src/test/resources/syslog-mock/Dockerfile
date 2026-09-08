# Tatt fra https://github.com/navikt/k9-verdikjede/blob/128f7fe84995a5c4ca1a78cb3d91d01747ba2ad2/saksbehandling/audit.nais/Dockerfile
FROM alpine:3.7
RUN apk add --no-cache socat
CMD ["/usr/bin/socat", "-u", "TCP4-LISTEN:6514,fork", "PIPE:/dev/stdout"]
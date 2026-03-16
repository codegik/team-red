FROM alpine:3.19

RUN apk add --no-cache curl jq

COPY register-schemas.sh /register-schemas.sh
COPY schemas/ /schemas/

RUN chmod +x /register-schemas.sh

ENTRYPOINT ["/register-schemas.sh"]

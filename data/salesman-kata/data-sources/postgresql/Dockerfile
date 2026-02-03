FROM node:20-alpine

WORKDIR /app

COPY data-sources/postgresql/package*.json ./

RUN npm install

COPY data-sources/postgresql/src/ ./src/

CMD ["node", "src/index.js"]
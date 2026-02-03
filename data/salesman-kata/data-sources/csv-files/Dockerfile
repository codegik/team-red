FROM node:20-alpine

WORKDIR /app

COPY ./data-sources/csv-files/package*.json ./

RUN npm install

COPY ./data-sources/csv-files/src/ ./src/

CMD ["node", "src/index.js"]
FROM mcr.microsoft.com/playwright:v1.59.1-jammy

WORKDIR /work

COPY package.json playwright.config.js esup-emargement.spec.js ./

RUN npm install

CMD ["npm", "run", "test"]


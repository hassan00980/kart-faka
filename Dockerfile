# ============================================================
#   كرت فكة — تشغيل أونلاين (Render / Railway / أي سيرفر Docker)
#   بناء: docker build -t kart-faka .
#   تشغيل: docker run -p 3000:3000 kart-faka
#   ============================================================
FROM node:18-alpine

WORKDIR /app

# نسخ ملفات التبعيات أولًا للاستفادة من الكاش
COPY package.json package-lock.json* ./
RUN npm install --omit=dev

# نسخ باقي المشروع (android/ و node_modules/ مستثنات في .dockerignore)
COPY . .

ENV PORT=3000
ENV NODE_ENV=production
EXPOSE 3000

CMD ["node", "server.js"]
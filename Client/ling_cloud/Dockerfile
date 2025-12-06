# 使用华为云Node.js镜像
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/node:22-alpine3.22

# 设置工作目录
WORKDIR /app

# 复制所有文件到容器中
COPY . .

# 安装依赖
RUN npm install

# 设置生产环境变量并执行构建
ENV NODE_ENV=production
RUN npm run build

# 全局安装serve
RUN npm install -g serve

# 暴露端口
EXPOSE 5173

# 启动服务
CMD ["serve", "-s", "dist", "-l", "5173"]
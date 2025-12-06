# 使用OpenJDK 21作为基础镜像
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:21

# 设置工作目录
WORKDIR /app

# 复制构建的JAR文件到容器中
COPY ling_cloud/target/ling_cloud-0.0.1-SNAPSHOT.jar app.jar

# 暴露8080端口
EXPOSE 8080

# 运行应用程序
ENTRYPOINT ["java", "-jar", "app.jar"]
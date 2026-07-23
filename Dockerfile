
# ---------- 运行阶段：只保留 JRE 和最终 JAR，运行 Spring Boot ----------
# Compose 启动前先在宿主机执行：mvn -B -DskipTests clean package
# Docker Desktop 内访问 Maven 仓库会发生间歇性读取超时，因此镜像构建只负责
# 封装已验证的产物，不在构建过程中下载任何 Java 依赖。
FROM eclipse-temurin:8-jre-jammy
WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --create-home spring \
    && mkdir -p /data/hmdp/imgs \
    && chown -R spring:spring /app /data/hmdp

COPY --chown=spring:spring target/hmdp-1.0-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
EXPOSE 8081
USER spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

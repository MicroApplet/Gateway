#指定基础镜像
FROM jre:25
EXPOSE 80
COPY ./*.jar /
# 赋予执行权限
RUN chmod 755 -R /app.jar
# 设置
ENTRYPOINT ["java","-Xms256m", "-Xmx1g", "-jar", "/app.jar"]
MAINTAINER AsialJim
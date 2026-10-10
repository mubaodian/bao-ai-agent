# 使用预装 Maven 和 JDK21 的镜像
FROM maven:3.9-amazoncorretto-21
WORKDIR /app

# 安装 Node.js：此基础镜像只有 JDK + Maven，Linux 上并不存在 npx，
# 高德 MCP Server 是 Node 程序，不装 Node 应用启动时会报「No such file or directory」
# 包名在不同 AL2023 版本下可能是 nodejs 或 nodejs20，这里做一次兼容
RUN (dnf install -y nodejs npm || dnf install -y nodejs20 nodejs20-npm) \
    && node -v && npm -v

# 构建期预装 MCP Server：容器文件系统是易失的，
# 若靠运行时 npx -y 拉包，每次冷启动都要访问 npm registry
# command -v 作为断言，bin 不在 PATH 上就让构建失败，而不是等到启动才暴露
RUN npm install -g @amap/amap-maps-mcp-server \
    && command -v mcp-amap

# 只复制必要的源代码和配置文件
COPY pom.xml .
COPY src ./src

# 使用 Maven 执行打包
RUN mvn clean package -DskipTests

# 暴露应用端口
EXPOSE 8123

# 使用生产环境配置启动应用
CMD ["java", "-jar", "/app/target/baodian-ai-agent-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=prod"]

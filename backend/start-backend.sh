#!/bin/bash

# 设置JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
echo "JAVA_HOME设置为: $JAVA_HOME"

# 设置PATH
export PATH=$JAVA_HOME/bin:$PATH

# 进入后端目录
cd "/Users/yinxiaogou/Desktop/SaySomething 7/YouthMind-backend-new-frontend 2/backend"

echo "启动后端服务器..."
echo "Java版本:"
java -version

echo "开始启动Spring Boot应用..."
mvn spring-boot:run

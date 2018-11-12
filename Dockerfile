FROM java:8-alpine

COPY target/uberjar/myapp.jar /myapp/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/myapp/app.jar"]

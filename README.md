# Kateway

Kateway is a gateway service to redirect network traffic to microservices while also adding additional features necessary for reliable production deployments.  

## Quickstart
Build it using gradle to create a fat jar with all dependencies. 

```bash
gradle build
java -jar build/lib/kateway-0.0.1.jar
```

### Add a service

**example-service.json**
```json
{"name": "example", "path": "/example", "targets": [{"url": "http://example.com:8080"}]}
```

```bash
curl -XPOST http://localhost:8081/services -H "Content-Type: application/json" --data "@example-service.json" 
```

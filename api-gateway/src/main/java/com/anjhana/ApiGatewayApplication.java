package com.anjhana;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — single entry point for all client requests.
 * Routes to appropriate microservices.
 * Add: rate limiting, JWT validation, CORS, request logging.
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) 
    { 
    	SpringApplication.run(ApiGatewayApplication.class, args); 
    }
}

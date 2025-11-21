package com.example.controller;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application Configuration
 * Base path for all REST endpoints: /api
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    // No additional configuration needed
    // JAX-RS will automatically discover all @Path annotated classes
}

// Made with Bob

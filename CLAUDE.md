# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Social Media Intelligence is a Spring Boot 4.0.1 application built with Java 25 and Maven. The project is designed to
collect and analyze social media data from various platforms.

**Base Package**: `com.media.intelligence`

## Technology Stack

- Spring Boot 4.0.1
- Java 25
- Maven (build tool)
- Lombok (annotation processor for boilerplate code)
- Spring DevTools (runtime development tools)
- Spring Boot Docker Compose support

## Build and Development Commands

### Building the Application

```bash
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=SocialMediaIntelligenceApplicationTests

# Run a specific test method
mvn test -Dtest=ClassName#methodName
```

### Packaging

```bash
mvn package
```

### Cleaning Build Artifacts

```bash
mvn clean
```

## Project Structure

- `src/main/java/com/media/intelligence/` - Main application source code
    - `SocialMediaIntelligenceApplication.java` - Spring Boot application entry point
- `src/main/resources/` - Configuration files
    - `application.properties` - Application configuration
- `src/test/java/com/media/intelligence/` - Test source code
- `pom.xml` - Maven project configuration and dependencies

## Annotation Processing

The project uses annotation processors for:

- **Spring Boot Configuration Processor** - Generates metadata for configuration properties
- **Lombok** - Reduces boilerplate code (getters, setters, constructors, etc.)

Both are configured in the Maven compiler plugin and must be present for the project to compile correctly.

## Current Branch Structure

- Main branch: `main`
- Current working branch: `project-setup-and-instagram-connector` (suggests Instagram integration is planned or in
  progress)

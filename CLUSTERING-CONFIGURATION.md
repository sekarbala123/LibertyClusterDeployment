# WebSphere Liberty Cluster Configuration Guide

## Overview

This document explains the clustering configuration implemented in the Liberty Cluster Deployment project. The cluster uses **WebSphere Liberty Collective** for centralized management.

## Key Features Configured

### 1. Collective Features

All three servers have the appropriate collective feature enabled:

**Controller:**
```xml
<featureManager>
    <feature>javaee-8.0</feature>
    <feature>adminCenter-1.0</feature>
    <feature>restConnector-2.0</feature>
    <feature>collectiveController-1.0</feature>
    <feature>appSecurity-2.0</feature>
</featureManager>
```

**Members:**
```xml
<featureManager>
    <feature>javaee-8.0</feature>
    <feature>adminCenter-1.0</feature>
    <feature>restConnector-2.0</feature>
    <feature>collectiveMember-1.0</feature>
    <feature>appSecurity-2.0</feature>
</featureManager>
```

**What it does:**
- Enables centralized management of Liberty servers
- Allows members to join a collective managed by a controller
- Provides Admin Center for web-based administration

### 2. HTTP Session Configuration

Each server has a unique `cloneId` for session identification:

**Controller:**
```xml
<httpSession cloneId="controller" 
             cookieName="JSESSIONID"
             cookiePath="/"
             invalidateOnUnauthorizedSessionRequestException="true"/>
```

**Member 1:**
```xml
<httpSession cloneId="member1" 
             cookieName="JSESSIONID"
             cookiePath="/"
             invalidateOnUnauthorizedSessionRequestException="true"/>
```

**Member 2:**
```xml
<httpSession cloneId="member2" 
             cookieName="JSESSIONID"
             cookiePath="/"
             invalidateOnUnauthorizedSessionRequestException="true"/>
```

**Configuration Details:**
- `cloneId`: Unique identifier for each server in the cluster
- `cookieName`: Standard session cookie name (JSESSIONID)
- `cookiePath`: Session cookie applies to entire application
- `invalidateOnUnauthorizedSessionRequestException`: Security setting

---

**Last Updated:** 2025-11-17
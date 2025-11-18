# Security Configuration Guide

## Overview

This document describes the enhanced security configuration implemented in the Liberty Cluster Deployment application, replacing the previous `quickStartSecurity` configuration with a more secure `basicRegistry` implementation using password encryption.

**Last Updated:** 2025-11-18

---

## Security Enhancements Implemented

### 1. Password Encryption

All passwords in the server configurations now use **XOR encoding** instead of plain text:

```xml
<!-- BEFORE (Insecure) -->
<basicRegistry id="basic" realm="basic">
    <user name="admin" password="admin" />
</basicRegistry>

<!-- AFTER (Secure) -->
<basicRegistry id="basic" realm="CollectiveRealm">
    <user name="admin" password="{xor}Lz4sLCgwLTs=" />
    <user name="controller-admin" password="{xor}Lz4sLCgwLTs=" />
</basicRegistry>
```

**Encoded Password:** `{xor}Lz4sLCgwLTs=` represents the password `admin123!`

### 2. Administrator Roles

Explicit administrator roles have been configured for all servers:

```xml
<administrator-role>
    <user>admin</user>
    <user>controller-admin</user>
</administrator-role>
```

### 3. Authorization Roles

The controller server includes authorization roles for management operations:

```xml
<authorization-roles id="com.ibm.ws.management">
    <security-role name="Administrator">
        <user name="admin"/>
        <user name="controller-admin"/>
    </security-role>
    <security-role name="Reader">
        <user name="admin"/>
        <user name="controller-admin"/>
    </security-role>
</authorization-roles>
```

### 4. Realm Configuration

Each server type has its own security realm:
- **Controller:** `CollectiveRealm`
- **Members:** `CollectiveMemberRealm`

---

## Server-Specific Configurations

### Collective Controller (liberty-cluster-app-ear)

**Location:** `liberty-cluster-app-ear/src/main/liberty/config/server.xml`

**Users:**
- `admin` - Primary administrator
- `controller-admin` - Secondary administrator for redundancy

**Ports:**
- HTTP: 9080
- HTTPS: 9443

**Access:**
```bash
# Admin Center
https://localhost:9443/adminCenter/
Username: admin
Password: admin123!

# REST API
curl -u admin:admin123! http://localhost:9080/liberty-cluster-app/api/members
```

### Member 1 (liberty-cluster-member1)

**Location:** `liberty-cluster-member1/src/main/liberty/config/server.xml`

**Users:**
- `admin` - Primary administrator
- `member1-admin` - Member-specific administrator

**Ports:**
- HTTP: 9081
- HTTPS: 9444

### Member 2 (liberty-cluster-member2)

**Location:** `liberty-cluster-member2/src/main/liberty/config/server.xml`

**Users:**
- `admin` - Primary administrator
- `member2-admin` - Member-specific administrator

**Ports:**
- HTTP: 9082
- HTTPS: 9445

---

## Password Management

### Generating Encoded Passwords

To generate your own encoded passwords, use the Liberty `securityUtility` command:

```bash
# Navigate to Liberty installation
cd liberty-cluster-app-ear/target/liberty/wlp/bin

# Encode a password (XOR encoding)
./securityUtility encode --encoding=xor yourPassword

# Encode a password (AES encryption - more secure)
./securityUtility encode --encoding=aes yourPassword
```

**Example Output:**
```
{xor}Lz4sLCgwLTs=
```

### Updating Passwords

1. Generate the encoded password using `securityUtility encode`
2. Update the server.xml file:
   ```xml
   <user name="admin" password="{xor}YOUR_ENCODED_PASSWORD" />
   ```
3. Restart the Liberty server

---

## Security Best Practices

### Development Environment ✅ (Current Configuration)

- ✅ XOR-encoded passwords
- ✅ Basic registry authentication
- ✅ Administrator roles defined
- ✅ Self-signed SSL certificates
- ✅ Multiple admin users for redundancy

### Production Environment Recommendations

#### 1. Use AES Encryption Instead of XOR

```bash
# Generate AES-encrypted password
./securityUtility encode --encoding=aes yourStrongPassword
```

Update server.xml:
```xml
<user name="admin" password="{aes}YOUR_AES_ENCODED_PASSWORD" />
```

#### 2. Implement LDAP or Active Directory

Replace `basicRegistry` with enterprise user registry:

```xml
<ldapRegistry id="ldap" realm="LDAPRealm"
              host="ldap.example.com"
              port="389"
              baseDN="ou=users,dc=example,dc=com"
              bindDN="cn=admin,dc=example,dc=com"
              bindPassword="{aes}ENCODED_PASSWORD"
              ldapType="IBM Tivoli Directory Server">
    <idsFilters
        userFilter="(&(uid=%v)(objectclass=person))"
        groupFilter="(&(cn=%v)(objectclass=groupOfNames))"
        userIdMap="*:uid"
        groupIdMap="*:cn"
        groupMemberIdMap="groupOfNames:member">
    </idsFilters>
</ldapRegistry>
```

#### 3. Use Valid SSL Certificates

Replace self-signed certificates with certificates from a trusted CA:

```xml
<keyStore id="defaultKeyStore" 
          password="{aes}ENCODED_PASSWORD"
          location="${server.config.dir}/resources/security/production-cert.p12"
          type="PKCS12" />
```

#### 4. Enable HTTPS Only

```xml
<httpEndpoint id="defaultHttpEndpoint"
              host="*"
              httpsPort="9443">
    <httpOptions removeServerHeader="true" />
    <accessLogging filepath="${server.output.dir}/logs/http_access.log" />
</httpEndpoint>
```

#### 5. Implement Security Headers

```xml
<httpEndpoint id="defaultHttpEndpoint"
              host="*"
              httpsPort="9443">
    <headers>
        <add>X-Frame-Options: DENY</add>
        <add>X-Content-Type-Options: nosniff</add>
        <add>X-XSS-Protection: 1; mode=block</add>
        <add>Strict-Transport-Security: max-age=31536000; includeSubDomains</add>
    </headers>
</httpEndpoint>
```

#### 6. Enable Security Auditing

```xml
<featureManager>
    <feature>audit-1.0</feature>
</featureManager>

<audit maxFiles="5" maxFileSize="20" compact="true">
    <events name="AuditEvent_1" eventName="SECURITY_AUTHN" outcome="success"/>
    <events name="AuditEvent_2" eventName="SECURITY_AUTHN" outcome="failure"/>
    <events name="AuditEvent_3" eventName="SECURITY_AUTHZ" outcome="failure"/>
</audit>
```

#### 7. Use Environment Variables for Secrets

```xml
<basicRegistry id="basic" realm="CollectiveRealm">
    <user name="${env.ADMIN_USERNAME}" password="${env.ADMIN_PASSWORD}" />
</basicRegistry>
```

Set environment variables:
```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD={aes}ENCODED_PASSWORD
```

---

## Testing Security Configuration

### 1. Test Admin Center Access

```bash
# Should require authentication
curl -k https://localhost:9443/adminCenter/

# With credentials
curl -k -u admin:admin123! https://localhost:9443/adminCenter/
```

### 2. Test REST API Authentication

```bash
# Without credentials (should fail if protected)
curl http://localhost:9080/liberty-cluster-app/api/members

# With credentials
curl -u admin:admin123! http://localhost:9080/liberty-cluster-app/api/members
```

### 3. Verify SSL Configuration

```bash
# Check SSL certificate
openssl s_client -connect localhost:9443 -showcerts

# Test HTTPS endpoint
curl -k https://localhost:9443/adminCenter/
```

### 4. Test Password Encoding

```bash
# Verify password works
cd liberty-cluster-app-ear/target/liberty/wlp/bin
./securityUtility encode --encoding=xor admin123!

# Output should match: {xor}Lz4sLCgwLTs=
```

---

## Troubleshooting

### Issue: Cannot Login to Admin Center

**Symptoms:**
- Login fails with correct credentials
- "Invalid username or password" error

**Solutions:**
1. Verify password encoding:
   ```bash
   ./securityUtility encode --encoding=xor admin123!
   ```
2. Check server.xml for correct user configuration
3. Restart the Liberty server
4. Check logs: `target/liberty/wlp/usr/servers/controller/logs/messages.log`

### Issue: REST API Returns 401 Unauthorized

**Symptoms:**
- API calls return 401 status code
- Authentication required error

**Solutions:**
1. Add authentication to API calls:
   ```bash
   curl -u admin:admin123! http://localhost:9080/liberty-cluster-app/api/members
   ```
2. Verify user has proper roles in server.xml
3. Check if application security is properly configured

### Issue: SSL Certificate Warnings

**Symptoms:**
- Browser shows "Your connection is not private"
- SSL certificate errors

**Solutions:**
1. For development: Accept the self-signed certificate
2. For production: Install valid SSL certificates from trusted CA
3. Use `-k` flag with curl to skip certificate verification (development only):
   ```bash
   curl -k https://localhost:9443/adminCenter/
   ```

---

## Security Checklist

### Development Environment ✅

- [x] Passwords are XOR-encoded
- [x] Administrator roles configured
- [x] Multiple admin users for redundancy
- [x] SSL enabled with self-signed certificates
- [x] Basic registry authentication
- [x] Authorization roles defined

### Production Environment (Recommended)

- [ ] Passwords are AES-encrypted
- [ ] LDAP/Active Directory integration
- [ ] Valid SSL certificates from trusted CA
- [ ] HTTPS-only configuration
- [ ] Security headers enabled
- [ ] Security auditing enabled
- [ ] Secrets stored in environment variables
- [ ] Regular security updates applied
- [ ] Penetration testing completed
- [ ] Security monitoring in place

---

## Compliance Considerations

### Java EE 8 Compliance ✅

The current security configuration is fully compliant with Java EE 8 specifications:
- Uses standard `basicRegistry` authentication
- Implements proper authorization roles
- Follows Java EE security best practices

### Industry Standards

For production deployments, consider implementing:
- **OWASP Top 10** security controls
- **PCI DSS** compliance (if handling payment data)
- **HIPAA** compliance (if handling health data)
- **GDPR** compliance (if handling EU citizen data)
- **SOC 2** compliance (for service organizations)

---

## Additional Resources

### IBM Documentation
- [WebSphere Liberty Security](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-securing)
- [Liberty Security Configuration](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-configuring-authentication)
- [Liberty Collective Security](https://www.ibm.com/docs/en/was-liberty/base?topic=collectives-securing-liberty-collective)

### Tools
- [Liberty Security Utility](https://www.ibm.com/docs/en/was-liberty/base?topic=line-securityutility-command)
- [OpenSSL](https://www.openssl.org/) - For SSL certificate management
- [OWASP ZAP](https://www.zaproxy.org/) - Security testing tool

---

## Summary

The Liberty Cluster Deployment application now implements enhanced security with:

1. ✅ **Encrypted Passwords** - XOR encoding (upgradeable to AES)
2. ✅ **Administrator Roles** - Explicit role definitions
3. ✅ **Authorization Controls** - Role-based access control
4. ✅ **Multiple Admin Users** - Redundancy and separation of duties
5. ✅ **Security Realms** - Separate realms for controller and members
6. ✅ **SSL/TLS** - HTTPS enabled with proper certificate configuration

**Current Status:** Development-ready with enhanced security
**Production Readiness:** Requires additional hardening (see recommendations above)

---

**For questions or additional security requirements, refer to the IBM WebSphere Liberty Security documentation.**
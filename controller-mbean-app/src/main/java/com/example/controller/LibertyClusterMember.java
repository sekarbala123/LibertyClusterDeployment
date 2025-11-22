package com.example.controller;

/**
 * Represents a Liberty Cluster Member parsed from ClusterManager MBean tuple
 * The tuple format from listMembers is: "host,hostName,httpPort,httpsPort,serverName,state,userDir"
 */
public class LibertyClusterMember {
    private String host;
    private String hostName;
    private String httpPort;
    private String httpsPort;
    private String serverName;
    private String state;
    private String userDir;
    
    public LibertyClusterMember() {
    }
    
    public LibertyClusterMember(String host, String hostName, String httpPort, String httpsPort, 
                                String serverName, String state, String userDir) {
        this.host = host;
        this.hostName = hostName;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.serverName = serverName;
        this.state = state;
        this.userDir = userDir;
    }
    
    /**
     * Parse a cluster member tuple string returned by ClusterManager.listMembers()
     * Format: "host,hostName,httpPort,httpsPort,serverName,state,userDir"
     * 
     * @param tuple The tuple string to parse
     * @return LibertyClusterMember object
     */
    public static LibertyClusterMember parseClusterMemberTuple(String tuple) {
        if (tuple == null || tuple.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = tuple.split(",");
        if (parts.length < 7) {
            // Handle incomplete tuples
            LibertyClusterMember member = new LibertyClusterMember();
            if (parts.length > 0) member.setHost(parts[0]);
            if (parts.length > 1) member.setHostName(parts[1]);
            if (parts.length > 2) member.setHttpPort(parts[2]);
            if (parts.length > 3) member.setHttpsPort(parts[3]);
            if (parts.length > 4) member.setServerName(parts[4]);
            if (parts.length > 5) member.setState(parts[5]);
            if (parts.length > 6) member.setUserDir(parts[6]);
            return member;
        }
        
        return new LibertyClusterMember(
            parts[0].trim(),  // host
            parts[1].trim(),  // hostName
            parts[2].trim(),  // httpPort
            parts[3].trim(),  // httpsPort
            parts[4].trim(),  // serverName
            parts[5].trim(),  // state
            parts[6].trim()   // userDir
        );
    }
    
    // Getters and Setters
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    public String getHttpPort() {
        return httpPort;
    }
    
    public void setHttpPort(String httpPort) {
        this.httpPort = httpPort;
    }
    
    public String getHttpsPort() {
        return httpsPort;
    }
    
    public void setHttpsPort(String httpsPort) {
        this.httpsPort = httpsPort;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getUserDir() {
        return userDir;
    }
    
    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }
    
    @Override
    public String toString() {
        return "LibertyClusterMember{" +
                "host='" + host + '\'' +
                ", hostName='" + hostName + '\'' +
                ", httpPort='" + httpPort + '\'' +
                ", httpsPort='" + httpsPort + '\'' +
                ", serverName='" + serverName + '\'' +
                ", state='" + state + '\'' +
                ", userDir='" + userDir + '\'' +
                '}';
    }
}

// Made with Bob

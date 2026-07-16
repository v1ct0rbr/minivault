package com.victorqueiroga.minivault.dto;

import jakarta.validation.constraints.NotBlank;

public class DatabaseCredentials {

    @NotBlank
    private String host;

    private int port;

    @NotBlank
    private String databaseName;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String databaseType;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }
}

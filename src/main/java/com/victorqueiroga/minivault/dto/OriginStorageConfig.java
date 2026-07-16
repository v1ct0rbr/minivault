package com.victorqueiroga.minivault.dto;

public class OriginStorageConfig {

    private String type;

    private String localPath;

    private String endpoint;
    private String bucketName;
    private String region;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleEnabled;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public boolean isPathStyleEnabled() { return pathStyleEnabled; }
    public void setPathStyleEnabled(boolean pathStyleEnabled) { this.pathStyleEnabled = pathStyleEnabled; }
}

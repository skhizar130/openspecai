package com.sk.openspecai.auth;

public interface TokenProvider {

    public String getToken();

    public String getOwner();
}

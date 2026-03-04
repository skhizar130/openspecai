package com.sk.openspecai.auth;

import org.springframework.stereotype.Component;

@Component
public class EnvTokenProvider implements TokenProvider {

    @Override
    public String getToken() {
        return System.getenv("SWAGGERHUB_API_KEY");
    }

    @Override
    public String getOwner() {
        return System.getenv("SWAGGERHUB_OWNER");
    }

}

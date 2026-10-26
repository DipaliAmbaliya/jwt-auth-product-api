package com.onedao.auth.security;

import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store of revoked token IDs (jti). Entries are purged once the token would have expired anyway. */
@Component
public class TokenBlacklist {

    private final Map<String, Date> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Date expiresAt) {
        Date now = new Date();
        revoked.entrySet().removeIf(e -> e.getValue().before(now));
        revoked.put(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        return jti != null && revoked.containsKey(jti);
    }
}

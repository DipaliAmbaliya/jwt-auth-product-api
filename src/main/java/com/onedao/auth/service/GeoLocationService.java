package com.onedao.auth.service;

import com.onedao.auth.exception.ForbiddenCountryException;
import com.onedao.auth.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the caller's country from the client IP (ip-api.com) and blocks sign-ups from restricted countries.
 * For loopback/private IPs (local testing) the provider is queried without an IP, so it uses the
 * public IP of the machine running this server, i.e. the developer's "current location".
 */
@Service
public class GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);

    private final boolean enabled;
    private final boolean failOpen;
    private final String providerUrl;
    private final Set<String> blocked;
    private final RestClient restClient;

    public GeoLocationService(@Value("${app.geo.enabled}") boolean enabled,
                              @Value("${app.geo.fail-open}") boolean failOpen,
                              @Value("${app.geo.provider-url}") String providerUrl,
                              @Value("${app.geo.blocked-countries}") List<String> blocked) {
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.providerUrl = providerUrl.endsWith("/") ? providerUrl : providerUrl + "/";
        this.blocked = blocked.stream().map(s -> s.trim().toUpperCase()).collect(Collectors.toSet());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Throws ForbiddenCountryException if the client's country is in the blocked list. */
    @SuppressWarnings("unchecked")
    public void assertCountryAllowed(String clientIp) {
        if (!enabled) return;
        try {
            String target = isPublicIp(clientIp) ? clientIp : "";
            Map<String, Object> res = restClient.get()
                    .uri(providerUrl + target + "?fields=status,message,countryCode,country")
                    .retrieve().body(Map.class);

            if (res == null || !"success".equals(res.get("status")) || res.get("countryCode") == null) {
                throw new IllegalStateException("Geo provider returned no country");
            }
            String code = String.valueOf(res.get("countryCode")).toUpperCase();
            log.info("Sign-up attempt from country {}", code);
            if (blocked.contains(code)) {
                throw new ForbiddenCountryException("Sign-up is not allowed from your country (" + res.get("country") + ")");
            }
        } catch (ForbiddenCountryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Country lookup failed: {}", e.getMessage());
            if (!failOpen) {
                throw new ServiceUnavailableException("Unable to verify your location right now. Please try again later.");
            }
        }
    }

    private boolean isPublicIp(String ip) {
        try {
            if (ip == null || ip.isBlank()) return false;
            InetAddress a = InetAddress.getByName(ip);
            return !(a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress());
        } catch (Exception e) {
            return false;
        }
    }
}

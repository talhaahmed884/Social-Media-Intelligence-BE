package com.media.intelligence.social_account.oauth;

import com.media.intelligence.social_account.config.SocialAccountProperties;
import com.media.intelligence.social_account.exception.SocialAccountErrorCode;
import com.media.intelligence.social_account.exception.SocialAccountException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues and verifies the OAuth {@code state} parameter.
 * <p>
 * The state is a signed token {@code base64url(userId|platform|expiresAt|nonce).base64url(HMAC-SHA256)},
 * so the callback can tell which user started the flow without server-side storage, and a forged or
 * expired state is rejected. Works across instances when {@code social-account.oauth.state-secret} is set.
 */
@Slf4j
@Component
public class OAuthStateService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration ttl;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthStateService(SocialAccountProperties properties, Clock clock) {
        this.secret = resolveSecret(properties.oauth().stateSecret());
        this.ttl = properties.oauth().stateTtl();
        this.clock = clock;
    }

    /**
     * Issue a state for a user starting the OAuth flow.
     *
     * @param userId   user who will own the linked account
     * @param platform platform being linked
     * @return signed state value
     */
    public String issue(UUID userId, SocialPlatform platform) {
        byte[] nonce = new byte[16];
        secureRandom.nextBytes(nonce);

        String payload = String.join("|",
                userId.toString(),
                platform.name(),
                String.valueOf(clock.instant().plus(ttl).getEpochSecond()),
                ENCODER.encodeToString(nonce));

        return ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + ENCODER.encodeToString(sign(payload));
    }

    /**
     * Verify a state from the OAuth callback.
     *
     * @param state            state from the redirect
     * @param expectedPlatform platform of the callback endpoint
     * @return user who started the flow
     * @throws SocialAccountException INVALID_OAUTH_STATE if missing, forged, for another platform, or expired
     */
    public UUID verify(String state, SocialPlatform expectedPlatform) {
        try {
            String[] parts = state.split("\\.");
            if (parts.length != 2) {
                throw invalid("malformed");
            }

            String payload = new String(DECODER.decode(parts[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(payload), DECODER.decode(parts[1]))) {
                throw invalid("bad signature");
            }

            String[] fields = payload.split("\\|");
            if (fields.length != 4) {
                throw invalid("malformed payload");
            }
            if (!expectedPlatform.name().equals(fields[1])) {
                throw invalid("platform mismatch");
            }
            if (clock.instant().isAfter(Instant.ofEpochSecond(Long.parseLong(fields[2])))) {
                throw invalid("expired");
            }
            return UUID.fromString(fields[0]);

        } catch (SocialAccountException e) {
            throw e;
        } catch (RuntimeException e) {
            // null state, bad base64, bad number/UUID
            throw invalid(e.getClass().getSimpleName());
        }
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private SocialAccountException invalid(String reason) {
        log.warn("Rejected OAuth state: {}", reason);
        return new SocialAccountException(SocialAccountErrorCode.INVALID_OAUTH_STATE);
    }

    private byte[] resolveSecret(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured.getBytes(StandardCharsets.UTF_8);
        }
        log.info("social-account.oauth.state-secret not set; using a random secret (pending authorizations are lost on restart)");
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return random;
    }
}

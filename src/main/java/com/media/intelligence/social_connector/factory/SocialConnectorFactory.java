package com.media.intelligence.social_connector.factory;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialMediaProvider;
import com.media.intelligence.social_connector.provider.SocialOAuthProvider;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of social media connectors, keyed by platform.
 * <p>
 * **Factory Pattern**: Clients request a connector by platform and get the matching implementation.
 * Spring discovers every {@link SocialMediaProvider} bean, so adding a platform only requires a new
 * {@code @Component} connector; this class does not change. Connectors that also implement
 * {@link SocialOAuthProvider} are registered for the OAuth flow.
 * <p>
 * Connectors are stateless singletons, so the same instance is returned on every call.
 * <p>
 * **Usage**:
 * <pre>
 * SocialMediaProvider instagram = socialConnectorFactory.getConnector(SocialPlatform.INSTAGRAM);
 * SocialOAuthProvider instagramOAuth = socialConnectorFactory.getOAuthProvider(SocialPlatform.INSTAGRAM);
 * </pre>
 */
@Slf4j
@Component
public class SocialConnectorFactory {

    private final Map<SocialPlatform, SocialMediaProvider> connectors;
    private final Map<SocialPlatform, SocialOAuthProvider> oauthProviders;

    public SocialConnectorFactory(List<SocialMediaProvider> providers) {
        Map<SocialPlatform, SocialMediaProvider> registry = new EnumMap<>(SocialPlatform.class);
        Map<SocialPlatform, SocialOAuthProvider> oauthRegistry = new EnumMap<>(SocialPlatform.class);

        for (SocialMediaProvider provider : providers) {
            SocialMediaProvider existing = registry.putIfAbsent(provider.getPlatform(), provider);
            if (existing != null) {
                throw new IllegalStateException("Multiple connectors registered for " + provider.getPlatform()
                        + ": " + existing.getClass().getSimpleName() + ", " + provider.getClass().getSimpleName());
            }
            if (provider instanceof SocialOAuthProvider oauthProvider) {
                oauthRegistry.put(provider.getPlatform(), oauthProvider);
            }
        }

        this.connectors = Collections.unmodifiableMap(registry);
        this.oauthProviders = Collections.unmodifiableMap(oauthRegistry);
        log.info("Registered social connectors: {} (OAuth: {})", connectors.keySet(), oauthProviders.keySet());
    }

    /**
     * Get the connector for a platform.
     *
     * @param platform social media platform
     * @return connector instance
     * @throws SocialConnectorException if no connector is registered for the platform
     */
    public SocialMediaProvider getConnector(SocialPlatform platform) throws SocialConnectorException {
        return lookup(connectors, platform, "connector");
    }

    /**
     * Get the OAuth provider for a platform.
     *
     * @param platform social media platform
     * @return OAuth provider
     * @throws SocialConnectorException if the platform has no OAuth support
     */
    public SocialOAuthProvider getOAuthProvider(SocialPlatform platform) throws SocialConnectorException {
        return lookup(oauthProviders, platform, "OAuth provider");
    }

    /**
     * @return platforms that currently have a connector
     */
    public Set<SocialPlatform> getSupportedPlatforms() {
        return connectors.keySet();
    }

    private static <T> T lookup(Map<SocialPlatform, T> registry, SocialPlatform platform, String kind) {
        T value = platform != null ? registry.get(platform) : null;
        if (value == null) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.UNSUPPORTED_PLATFORM,
                    "No " + kind + " available for platform: " + platform
            );
        }
        return value;
    }
}

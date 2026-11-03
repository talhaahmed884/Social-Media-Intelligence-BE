package com.media.intelligence.social_connector.factory;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialMediaProvider;
import com.media.intelligence.social_connector.provider.SocialOAuthProvider;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@DisplayName("SocialConnectorFactory Tests")
public class SocialConnectorFactoryTest {

    private SocialMediaProvider instagram;

    @BeforeEach
    void setUp() {
        instagram = providerFor(SocialPlatform.INSTAGRAM);
    }

    @Test
    @DisplayName("getConnector: Should return the registered connector")
    void getConnector_ShouldReturnRegisteredConnector() {
        // Arrange
        SocialConnectorFactory factory = new SocialConnectorFactory(List.of(instagram));

        // Act & Assert
        assertSame(instagram, factory.getConnector(SocialPlatform.INSTAGRAM));
        assertEquals(Set.of(SocialPlatform.INSTAGRAM), factory.getSupportedPlatforms());
    }

    @Test
    @DisplayName("getConnector: Should throw UNSUPPORTED_PLATFORM for platforms without a connector")
    void getConnector_ShouldThrowForUnsupportedPlatform() {
        // Arrange
        SocialConnectorFactory factory = new SocialConnectorFactory(List.of(instagram));

        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                factory.getConnector(SocialPlatform.TWITTER));

        // Assert
        assertEquals(SocialConnectorErrorCode.UNSUPPORTED_PLATFORM, ex.getErrorCode());
    }

    @Test
    @DisplayName("getConnector: Should throw UNSUPPORTED_PLATFORM for null platform")
    void getConnector_ShouldThrowForNullPlatform() {
        // Arrange
        SocialConnectorFactory factory = new SocialConnectorFactory(List.of(instagram));

        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                factory.getConnector(null));

        // Assert
        assertEquals(SocialConnectorErrorCode.UNSUPPORTED_PLATFORM, ex.getErrorCode());
    }

    @Test
    @DisplayName("getOAuthProvider: Should register connectors that implement SocialOAuthProvider")
    void getOAuthProvider_ShouldRegisterOAuthCapableConnectors() {
        // Arrange
        SocialMediaProvider oauthCapable = mock(SocialMediaProvider.class,
                withSettings().extraInterfaces(SocialOAuthProvider.class));
        when(oauthCapable.getPlatform()).thenReturn(SocialPlatform.FACEBOOK);
        SocialConnectorFactory factory = new SocialConnectorFactory(List.of(instagram, oauthCapable));

        // Act & Assert
        assertSame(oauthCapable, factory.getOAuthProvider(SocialPlatform.FACEBOOK));
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                factory.getOAuthProvider(SocialPlatform.INSTAGRAM));
        assertEquals(SocialConnectorErrorCode.UNSUPPORTED_PLATFORM, ex.getErrorCode());
    }

    @Test
    @DisplayName("constructor: Should fail when two connectors claim the same platform")
    void constructor_ShouldFailOnDuplicatePlatform() {
        // Arrange
        SocialMediaProvider duplicate = providerFor(SocialPlatform.INSTAGRAM);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                new SocialConnectorFactory(List.of(instagram, duplicate)));
    }

    private SocialMediaProvider providerFor(SocialPlatform platform) {
        SocialMediaProvider provider = mock(SocialMediaProvider.class);
        when(provider.getPlatform()).thenReturn(platform);
        return provider;
    }
}

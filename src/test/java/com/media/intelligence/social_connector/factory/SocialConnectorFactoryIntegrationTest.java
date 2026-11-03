package com.media.intelligence.social_connector.factory;

import com.media.intelligence.social_connector.connector.instagram.InstagramConnector;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SocialConnectorFactory Integration Tests")
public class SocialConnectorFactoryIntegrationTest {

    @Autowired
    private SocialConnectorFactory factory;

    @Test
    @DisplayName("Spring context: Should register InstagramConnector as a bean")
    void context_ShouldRegisterInstagramConnector() {
        assertTrue(factory.getSupportedPlatforms().contains(SocialPlatform.INSTAGRAM));
        assertInstanceOf(InstagramConnector.class, factory.getConnector(SocialPlatform.INSTAGRAM));
    }
}

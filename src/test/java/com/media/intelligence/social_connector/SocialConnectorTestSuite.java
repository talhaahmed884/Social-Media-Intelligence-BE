package com.media.intelligence.social_connector;

import com.media.intelligence.social_connector.connector.instagram.InstagramConnectorTest;
import com.media.intelligence.social_connector.connector.instagram.InstagramErrorMapperTest;
import com.media.intelligence.social_connector.connector.instagram.InstagramResponseParserTest;
import com.media.intelligence.social_connector.exception.SocialConnectorExceptionTest;
import com.media.intelligence.social_connector.factory.SocialConnectorFactoryIntegrationTest;
import com.media.intelligence.social_connector.factory.SocialConnectorFactoryTest;
import com.media.intelligence.social_connector.resilience.RateLimiterTest;
import com.media.intelligence.social_connector.resilience.RetryPolicyTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite for the Social Connector domain.
 * <p>
 * Includes:
 * - Instagram connector (MockRestServiceServer, no network), error mapper, response parser
 * - Resilience (rate limiter, retry policy)
 * - Factory (unit + Spring wiring)
 * - Exception / error code mapping
 */
@Suite
@SuiteDisplayName("Social Connector Test Suite")
@SelectClasses({
        InstagramConnectorTest.class,
        InstagramErrorMapperTest.class,
        InstagramResponseParserTest.class,
        RateLimiterTest.class,
        RetryPolicyTest.class,
        SocialConnectorFactoryTest.class,
        SocialConnectorFactoryIntegrationTest.class,
        SocialConnectorExceptionTest.class
})
public class SocialConnectorTestSuite {
    // Test suite runner - no additional code needed
}

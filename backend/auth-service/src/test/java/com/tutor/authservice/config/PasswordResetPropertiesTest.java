package com.tutor.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetPropertiesTest {

    @Test
    void allowsTokenExposureInLocalProfile() {
        PasswordResetProperties properties = propertiesFor("local");
        properties.setExposeToken(true);

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void allowsTokenExposureInTestProfile() {
        PasswordResetProperties properties = propertiesFor("test");
        properties.setExposeToken(true);

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsTokenExposureOutsideLocalOrTestProfiles() {
        PasswordResetProperties properties = propertiesFor("prod");
        properties.setExposeToken(true);

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Password reset token exposure is allowed only in local or test profiles");
    }

    @Test
    void allowsDisabledTokenExposureInAnyProfile() {
        PasswordResetProperties properties = propertiesFor("prod");

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    private PasswordResetProperties propertiesFor(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return new PasswordResetProperties(environment);
    }
}

package com.tutor.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.password-reset")
@Getter
@Setter
public class PasswordResetProperties implements InitializingBean {

    private final Environment environment;

    private boolean exposeToken;

    public PasswordResetProperties(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (exposeToken && !environment.acceptsProfiles(Profiles.of("local", "test"))) {
            throw new IllegalStateException(
                    "Password reset token exposure is allowed only in local or test profiles"
            );
        }
    }
}

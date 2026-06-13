package com.tutor.crmservice.settings;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ToggleItem {
    private String toggleId;
    private String title;
    private String description;
    private boolean enabled;
}

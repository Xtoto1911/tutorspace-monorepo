package com.tutor.crmservice.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "client_settings")
@Getter
@Setter
@NoArgsConstructor
public class ClientSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String parentName;
    private String phone;
    private String email;
    private String city;
    private String timezone;
    private String language;

    @ElementCollection
    @CollectionTable(name = "client_notifications", joinColumns = @JoinColumn(name = "client_settings_id"))
    private List<ToggleItem> notifications = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "client_privacy", joinColumns = @JoinColumn(name = "client_settings_id"))
    private List<ToggleItem> privacy = new ArrayList<>();
}

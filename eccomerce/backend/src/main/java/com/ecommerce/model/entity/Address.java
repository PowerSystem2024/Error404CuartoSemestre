package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type; // "SHIPPING" o "BILLING"

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String company;

    @Column(name = "address_line_1", nullable = false)
    private String address;

    @Column(name = "address_line_2")
    private String address2;

    @Column(nullable = false)
    private String city;

    @Column
    private String state;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String country;

    @Column
    private String phone;

    @Column
    private String instructions;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDefault = false;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        fullAddress.append(address);
        if (address2 != null && !address2.trim().isEmpty()) {
            fullAddress.append(", ").append(address2);
        }
        fullAddress.append(", ").append(city);
        if (state != null && !state.trim().isEmpty()) {
            fullAddress.append(", ").append(state);
        }
        fullAddress.append(", ").append(zipCode);
        fullAddress.append(", ").append(country);
        return fullAddress.toString();
    }
}

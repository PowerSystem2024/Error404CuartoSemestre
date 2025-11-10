package com.ecommerce.service.interfaces;

import com.ecommerce.model.entity.Address;
import com.ecommerce.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface AddressService {

    // CRUD operations
    Address saveAddress(Address address);

    Optional<Address> findById(Long id);

    List<Address> findByUser(User user);

    List<Address> findByUserAndType(User user, String type);

    void deleteAddress(Long id);

    // Business logic
    Address createShippingAddress(User user, Address address);

    Address createBillingAddress(User user, Address address);

    Address updateAddress(Long id, Address address);

    Address setDefaultAddress(User user, Long addressId, String type);

    // Helper methods
    Optional<Address> getDefaultShippingAddress(User user);

    Optional<Address> getDefaultBillingAddress(User user);

    boolean hasAddresses(User user, String type);
}
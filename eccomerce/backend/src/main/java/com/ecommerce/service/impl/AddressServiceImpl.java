package com.ecommerce.service.impl;

import com.ecommerce.model.entity.Address;
import com.ecommerce.model.entity.User;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.service.interfaces.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public Address saveAddress(Address address) {
        return addressRepository.save(address);
    }

    @Override
    public Optional<Address> findById(Long id) {
        return addressRepository.findById(id);
    }

    @Override
    public List<Address> findByUser(User user) {
        try {
            List<Address> addresses = addressRepository.findByUser(user);
            return addresses != null ? addresses : new ArrayList<>();
        } catch (Exception e) {
            // Log the error and return empty list to prevent 500 errors
            // Error logging removed for production mode
            return new ArrayList<>();
        }
    }

    @Override
    public List<Address> findByUserAndType(User user, String type) {
        try {
            List<Address> addresses = addressRepository.findByUserAndTypeOrderByDefault(user, type);
            return addresses != null ? addresses : new ArrayList<>();
        } catch (Exception e) {
            // Log the error and return empty list to prevent 500 errors
            // Error logging removed for production mode
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        addressRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Address createShippingAddress(User user, Address address) {
        address.setUser(user);
        address.setType("SHIPPING");

        // If this is the first shipping address or marked as default, make it default
        if (address.getIsDefault() == null || !hasAddresses(user, "SHIPPING")) {
            address.setIsDefault(true);
            // Remove default flag from other shipping addresses
            unsetDefaultForOtherAddresses(user, "SHIPPING");
        } else if (address.getIsDefault()) {
            unsetDefaultForOtherAddresses(user, "SHIPPING");
        }

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address createBillingAddress(User user, Address address) {
        address.setUser(user);
        address.setType("BILLING");

        // If this is the first billing address or marked as default, make it default
        if (address.getIsDefault() == null || !hasAddresses(user, "BILLING")) {
            address.setIsDefault(true);
            // Remove default flag from other billing addresses
            unsetDefaultForOtherAddresses(user, "BILLING");
        } else if (address.getIsDefault()) {
            unsetDefaultForOtherAddresses(user, "BILLING");
        }

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address updateAddress(Long id, Address updatedAddress) {
        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Update fields
        existingAddress.setFirstName(updatedAddress.getFirstName());
        existingAddress.setLastName(updatedAddress.getLastName());
        existingAddress.setAddress(updatedAddress.getAddress());
        existingAddress.setAddress2(updatedAddress.getAddress2());
        existingAddress.setCity(updatedAddress.getCity());
        existingAddress.setZipCode(updatedAddress.getZipCode());
        existingAddress.setCountry(updatedAddress.getCountry());
        existingAddress.setPhone(updatedAddress.getPhone());
        existingAddress.setInstructions(updatedAddress.getInstructions());

        // Handle default flag
        if (updatedAddress.getIsDefault() != null && updatedAddress.getIsDefault()) {
            unsetDefaultForOtherAddresses(existingAddress.getUser(), existingAddress.getType());
            existingAddress.setIsDefault(true);
        } else if (updatedAddress.getIsDefault() != null) {
            existingAddress.setIsDefault(updatedAddress.getIsDefault());
        }

        return addressRepository.save(existingAddress);
    }

    @Override
    @Transactional
    public Address setDefaultAddress(User user, Long addressId, String type) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().equals(user) || !address.getType().equals(type)) {
            throw new RuntimeException("Address does not belong to user or wrong type");
        }

        // Remove default flag from other addresses of same type
        unsetDefaultForOtherAddresses(user, type);

        // Set this address as default
        address.setIsDefault(true);
        return addressRepository.save(address);
    }

    @Override
    public Optional<Address> getDefaultShippingAddress(User user) {
        return addressRepository.findByUserAndIsDefaultTrueAndType(user, "SHIPPING");
    }

    @Override
    public Optional<Address> getDefaultBillingAddress(User user) {
        return addressRepository.findByUserAndIsDefaultTrueAndType(user, "BILLING");
    }

    @Override
    public boolean hasAddresses(User user, String type) {
        return addressRepository.existsByUserAndType(user, type);
    }

    private void unsetDefaultForOtherAddresses(User user, String type) {
        Optional<Address> defaultAddress = addressRepository.findByUserAndIsDefaultTrueAndType(user, type);
        if (defaultAddress.isPresent()) {
            Address addr = defaultAddress.get();
            addr.setIsDefault(false);
            addressRepository.save(addr);
        }
    }
}
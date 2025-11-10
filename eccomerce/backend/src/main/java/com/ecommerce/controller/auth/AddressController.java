package com.ecommerce.controller.auth;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.response.AddressResponse;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.model.entity.Address;
import com.ecommerce.model.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.userdetails.UserDetailsImpl;
import com.ecommerce.service.interfaces.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/addresses")
@RequiredArgsConstructor
@Tag(name = "Direcciones", description = "Gestión de direcciones de envío y facturación")
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElse(null); // Return null instead of throwing exception
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener direcciones del usuario", description = "Obtiene todas las direcciones del usuario actual")
    public ResponseEntity<List<AddressResponse>> getUserAddresses(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Address> addresses = addressService.findByUser(user);
        List<AddressResponse> responses = addresses.stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/shipping")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener direcciones de envío", description = "Obtiene las direcciones de envío del usuario")
    public ResponseEntity<List<AddressResponse>> getShippingAddresses(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Address> addresses = addressService.findByUserAndType(user, "SHIPPING");
        List<AddressResponse> responses = addresses.stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/billing")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener direcciones de facturación", description = "Obtiene las direcciones de facturación del usuario")
    public ResponseEntity<List<AddressResponse>> getBillingAddresses(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Address> addresses = addressService.findByUserAndType(user, "BILLING");
        List<AddressResponse> responses = addresses.stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/shipping")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear dirección de envío", description = "Crea una nueva dirección de envío para el usuario")
    public ResponseEntity<AddressResponse> createShippingAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = new User();
        user.setId(userDetails.getId());

        Address address = mapToAddressEntity(request);
        Address savedAddress = addressService.createShippingAddress(user, address);
        AddressResponse response = mapToAddressResponse(savedAddress);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/billing")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear dirección de facturación", description = "Crea una nueva dirección de facturación para el usuario")
    public ResponseEntity<AddressResponse> createBillingAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = new User();
        user.setId(userDetails.getId());

        Address address = mapToAddressEntity(request);
        Address savedAddress = addressService.createBillingAddress(user, address);
        AddressResponse response = mapToAddressResponse(savedAddress);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar dirección", description = "Actualiza una dirección existente del usuario")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = new User();
        user.setId(userDetails.getId());

        Address updatedAddress = addressService.updateAddress(id, mapToAddressEntity(request));
        AddressResponse response = mapToAddressResponse(updatedAddress);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Establecer dirección por defecto", description = "Establece una dirección como la dirección por defecto")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable Long id,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = new User();
        user.setId(userDetails.getId());

        // Get the address to determine its type
        Address address = addressService.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        Address defaultAddress = addressService.setDefaultAddress(user, id, address.getType());
        AddressResponse response = mapToAddressResponse(defaultAddress);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar dirección", description = "Elimina una dirección del usuario")
    public ResponseEntity<MessageResponse> deleteAddress(
            @PathVariable Long id,
            Authentication authentication) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(new MessageResponse("Address deleted successfully"));
    }

    private Address mapToAddressEntity(AddressRequest request) {
        return Address.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .address(request.getAddress())
                .address2(request.getAddress2())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .instructions(request.getInstructions())
                .isDefault(request.getIsDefault())
                .build();
    }

    private AddressResponse mapToAddressResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setType(address.getType());
        response.setFirstName(address.getFirstName());
        response.setLastName(address.getLastName());
        response.setFullName(address.getFullName());
        response.setAddress(address.getAddress());
        response.setAddress2(address.getAddress2());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setZipCode(address.getZipCode());
        response.setCountry(address.getCountry());
        response.setFullAddress(address.getFullAddress());
        response.setPhone(address.getPhone());
        response.setInstructions(address.getInstructions());
        response.setIsDefault(address.getIsDefault());
        response.setCreatedAt(address.getCreatedAt());
        response.setUpdatedAt(address.getUpdatedAt());
        return response;
    }
}
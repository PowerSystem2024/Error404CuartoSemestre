package com.ecommerce.service.impl;

import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.dto.response.ProfileResponse;
import com.ecommerce.exception.EmailNotVerifiedException;
import com.ecommerce.model.entity.Address;
import com.ecommerce.model.entity.Profile;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.EmailVerificationStatus;
import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.interfaces.AddressService;
import com.ecommerce.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

        private final UserRepository userRepository;
        private final ProfileRepository profileRepository;
        private final AddressService addressService;

        @Override
        @Transactional
        public MessageResponse updateProfile(Long userId, UpdateProfileRequest updateProfileRequest) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                if (user.getEmailVerificationStatus() != EmailVerificationStatus.VERIFIED) {
                        throw new EmailNotVerifiedException("Debes verificar tu email antes de completar tu perfil");
                }

                Profile profile = profileRepository.findByUserId(user.getId())
                                .orElse(Profile.builder().user(user).build());

                profile.setFirstName(updateProfileRequest.getFirstName());
                profile.setLastName(updateProfileRequest.getLastName());
                profile.setPhone(updateProfileRequest.getPhone());

                profileRepository.save(profile);

                // Si se proporcionaron datos de dirección, crear una dirección de envío por
                // defecto
                if (updateProfileRequest.getAddress() != null && !updateProfileRequest.getAddress().trim().isEmpty()) {
                        Address address = Address.builder()
                                        .user(user)
                                        .type("SHIPPING")
                                        .firstName(updateProfileRequest.getFirstName())
                                        .lastName(updateProfileRequest.getLastName())
                                        .address(updateProfileRequest.getAddress())
                                        .city(updateProfileRequest.getCity())
                                        .zipCode(updateProfileRequest.getZipCode())
                                        .country(updateProfileRequest.getCountry())
                                        .phone(updateProfileRequest.getPhone())
                                        .isDefault(true)
                                        .build();

                        addressService.saveAddress(address);
                }

                return new MessageResponse("Perfil actualizado exitosamente");
        }

        @Override
        @Transactional(readOnly = true)
        public ProfileResponse getProfile(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                Profile profile = profileRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

                return ProfileResponse.builder()
                                .firstName(profile.getFirstName())
                                .lastName(profile.getLastName())
                                .phone(profile.getPhone())
                                .email(user.getEmail())
                                .username(user.getUsername())
                                .build();
        }

        @Override
        @Transactional
        public MessageResponse deactivateAccount(Long userId) {
                // log.info("Desactivando cuenta para usuario: {}", userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                // Desactivar el usuario (soft delete)
                user.setActive(false);
                userRepository.save(user);

                // log.info("Cuenta desactivada exitosamente para usuario: {}", userId);

                return new MessageResponse("Cuenta desactivada exitosamente");
        }
}
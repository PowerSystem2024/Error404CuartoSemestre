package com.ecommerce.service.interfaces;

import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.dto.response.ProfileResponse;

public interface ProfileService {

    MessageResponse updateProfile(Long userId, UpdateProfileRequest updateProfileRequest);

    ProfileResponse getProfile(Long userId);

    MessageResponse deactivateAccount(Long userId);
}
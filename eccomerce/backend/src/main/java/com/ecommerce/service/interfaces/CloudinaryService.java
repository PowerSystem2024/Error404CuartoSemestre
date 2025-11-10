package com.ecommerce.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface CloudinaryService {
    Map<String, Object> uploadImage(MultipartFile file, ImageFolder folder);

    String deleteImage(String publicId);

    enum ImageFolder {
        PRODUCTS,
        CATEGORIES
    }
}
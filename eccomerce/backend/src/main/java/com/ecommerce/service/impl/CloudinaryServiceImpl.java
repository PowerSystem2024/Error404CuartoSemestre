package com.ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.service.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder.products:ecommerce/products}")
    private String productsFolder;

    @Value("${cloudinary.folder.categories:ecommerce/categories}")
    private String categoriesFolder;

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, ImageFolder folder) {
        try {
            log.debug("Subiendo imagen a Cloudinary - Nombre: {}, Tamaño: {} bytes, Carpeta: {}",
                    file.getOriginalFilename(), file.getSize(), folder);

            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("El archivo debe ser una imagen");
            }

            // Validar tamaño (máximo 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("La imagen no puede ser mayor a 10MB");
            }

            // Obtener la carpeta correspondiente
            String targetFolder = getFolderForType(folder);

            // Configurar opciones de subida
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", targetFolder,
                    "resource_type", "image",
                    "quality", "auto",
                    "format", "webp",
                    "access_mode", "public");

            // Subir imagen
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            // log.info("Imagen subida exitosamente a Cloudinary - PublicID: {}, URL: {},
            // Carpeta: {}",
            // uploadResult.get("public_id"), uploadResult.get("secure_url"), targetFolder);

            return uploadResult;

        } catch (IOException e) {
            log.error("Error de I/O al subir imagen a Cloudinary", e);
            throw new RuntimeException("Error al procesar la imagen", e);
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new RuntimeException("Error al subir imagen a Cloudinary: " + e.getMessage(), e);
        }
    }

    private String getFolderForType(ImageFolder folder) {
        switch (folder) {
            case PRODUCTS:
                return productsFolder;
            case CATEGORIES:
                return categoriesFolder;
            default:
                return productsFolder; // fallback
        }
    }

    @Override
    public String deleteImage(String publicId) {
        try {
            log.debug("Eliminando imagen de Cloudinary - PublicID: {}", publicId);

            @SuppressWarnings("unchecked")
            Map<String, Object> deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            String result = (String) deleteResult.get("result");
            // log.info("Imagen eliminada de Cloudinary - PublicID: {}, Resultado: {}",
            // publicId, result);

            return result;

        } catch (Exception e) {
            log.error("Error al eliminar imagen de Cloudinary - PublicID: {}", publicId, e);
            throw new RuntimeException("Error al eliminar imagen de Cloudinary", e);
        }
    }

}
package com.ecommerce.controller.admin;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.service.interfaces.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/admin/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Images", description = "Endpoints para gestión de imágenes (Admin)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminImageController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen a Cloudinary", description = "Sube una imagen a Cloudinary y devuelve la información de la imagen subida. El parámetro 'folder' especifica el tipo de imagen (PRODUCTS, USERS, CATEGORIES)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "PRODUCTS") String folderType) {

        log.info("Solicitud de subida de imagen - Nombre: {}, Tamaño: {}, Tipo: {}",
                file.getOriginalFilename(), file.getSize(), folderType);

        try {
            // Convertir el string del folder a enum
            CloudinaryService.ImageFolder folder = CloudinaryService.ImageFolder.valueOf(folderType.toUpperCase());

            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, folder);

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Imagen subida exitosamente")
                    .data(uploadResult)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validación fallida al subir imagen: {} - FolderType: {}", e.getMessage(), folderType);

            String errorMessage;
            if (e.getMessage().contains("No enum constant")) {
                errorMessage = "Tipo de carpeta inválido: " + folderType
                        + ". Valores permitidos: PRODUCTS, USERS, CATEGORIES";
            } else {
                errorMessage = e.getMessage();
            }

            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message(errorMessage)
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error interno al subir imagen", e);

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error interno del servidor")
                    .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/delete/{publicId}")
    @Operation(summary = "Eliminar imagen de Cloudinary", description = "Elimina una imagen de Cloudinary usando su public ID")
    public ResponseEntity<ApiResponse<String>> deleteImage(@PathVariable String publicId) {

        log.info("Solicitud de eliminación de imagen - PublicID: {}", publicId);

        try {
            String result = cloudinaryService.deleteImage(publicId);

            ApiResponse<String> response = ApiResponse.<String>builder()
                    .success(true)
                    .message("Imagen eliminada exitosamente")
                    .data(result)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al eliminar imagen - PublicID: {}", publicId, e);

            ApiResponse<String> response = ApiResponse.<String>builder()
                    .success(false)
                    .message("Error al eliminar la imagen")
                    .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
/**
 * Utilidades para manejo de imágenes públicas de Cloudinary
 * Simplificado para usar imágenes públicas sin autenticación
 */

/**
 * Extrae el publicId de una URL de Cloudinary
 * @param cloudinaryUrl URL completa de Cloudinary
 * @returns publicId de la imagen
 */
export const extractPublicIdFromUrl = (cloudinaryUrl: string): string | null => {
    try {
        // URL típica: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
        const urlParts = cloudinaryUrl.split('/');
        const uploadIndex = urlParts.findIndex(part => part === 'upload');

        if (uploadIndex === -1) return null;

        // El publicId está después de 'upload' y antes de la extensión
        const publicIdWithVersion = urlParts.slice(uploadIndex + 1).join('/');
        const publicId = publicIdWithVersion.replace(/^v\d+\//, '').replace(/\.[^.]+$/, '');

        return publicId;
    } catch (error) {
        console.warn('Error extracting publicId from URL:', cloudinaryUrl, error);
        return null;
    }
};

/**
 * Valida si una URL es de Cloudinary
 * @param url URL a validar
 * @returns true si es una URL de Cloudinary
 */
export const isCloudinaryUrl = (url: string): boolean => {
    return url?.includes('cloudinary.com') || false;
};

/**
 * Procesa URLs de imágenes filtrando las válidas
 * @param imageUrls Array de URLs de imágenes
 * @returns Array de URLs válidas
 */
export const processImageUrls = (imageUrls: (string | undefined)[]): string[] => {
    return imageUrls.filter(url => url !== undefined && url.trim() !== '') as string[];
};

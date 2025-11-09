import React, { useState } from 'react';

interface PublicImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
    src: string;
    alt: string;
    fallbackSrc?: string;
}

/**
 * Componente de imagen simplificado para imágenes públicas de Cloudinary
 * con manejo de errores y fallback
 */
const SignedImage: React.FC<PublicImageProps> = ({
    src,
    alt,
    fallbackSrc = '/placeholder-product.jpg',
    ...props
}) => {
    const [currentSrc, setCurrentSrc] = useState<string>(src || fallbackSrc);
    const [isLoading, setIsLoading] = useState<boolean>(true);

    const handleError = (e: React.SyntheticEvent<HTMLImageElement, Event>) => {
        if (currentSrc !== fallbackSrc) {
            setCurrentSrc(fallbackSrc);
        }
        props.onError?.(e);
    };

    const handleLoad = (e: React.SyntheticEvent<HTMLImageElement, Event>) => {
        setIsLoading(false);
        props.onLoad?.(e);
    };

    return (
        <img
            {...props}
            src={currentSrc}
            alt={alt}
            style={{
                ...props.style,
                opacity: isLoading ? 0.7 : 1,
                transition: 'opacity 0.3s ease-in-out'
            }}
            onError={handleError}
            onLoad={handleLoad}
        />
    );
};

export default SignedImage;

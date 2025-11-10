import { initMercadoPago, Wallet } from '@mercadopago/sdk-react';
import { useEffect, useState } from 'react';

interface MercadoPagoWalletProps {
    preferenceId: string;
    onReady?: () => void;
    onError?: (error: any) => void;
}

const MercadoPagoWallet: React.FC<MercadoPagoWalletProps> = ({ preferenceId, onReady, onError }) => {
    const [, setIsReady] = useState(false);  // Simplificado, sin variable no usada
    const publicKey = import.meta.env.VITE_MP_PUBLIC_KEY;

    useEffect(() => {
        if (publicKey) {
            try {
                // Configuramos Mercado Pago
                initMercadoPago(publicKey, { locale: 'es-AR' });
                setIsReady(true);
                if (onReady) {
                    onReady();
                }
            } catch (error) {
                // Error handling removed for production mode
                if (onError) {
                    onError(error);
                }
            }
        } else {
            // Error handling removed for production mode
            if (onError) {
                onError(new Error('Mercado Pago public key is not defined.'));
            }
        }
    }, [publicKey, onReady, onError]);

    // Renderizamos siempre el contenedor del Wallet. Si no hay preferenceId aún,
    // mostramos el componente Wallet inicializado con cadena vacía (el SDK maneja internamente)
    // y colocamos una capa bloqueadora para impedir clicks hasta tener la preferencia.
    return (
        <div className="flex justify-center mt-6 relative">
            <Wallet
                initialization={{
                    preferenceId: preferenceId || '',
                    redirectMode: 'blank' // Configuración para abrir en nueva ventana
                }}
                onReady={() => {
                    // Success logging removed for production mode
                    if (onReady) onReady();
                }}
                onError={(error) => {
                    // Error handling removed for production mode
                    if (onError) {
                        onError(error);
                    }
                }}
            />

            {/* Overlay para bloquear interacción hasta que tengamos preferenceId */}
            {!preferenceId && (
                <div className="absolute inset-0 flex items-center justify-center bg-white/0 pointer-events-auto">
                    <div className="text-center text-sm text-muted-foreground bg-secondary/80 p-3 rounded">
                        Completa los datos y sal del campo Código Postal para activar el botón de Mercado Pago.
                    </div>
                </div>
            )}

            {/* Mostrar indicador de modo sandbox en desarrollo */}
            {import.meta.env.MODE === 'development' && (
                <div className="absolute top-0 right-0 bg-amber-500 text-white text-xs px-2 py-1 rounded-bl">
                    SANDBOX
                </div>
            )}
        </div>
    );
};

export default MercadoPagoWallet;


import { useUserStore } from '../stores/userStore';
import AddressManager from '../components/AddressManager';

const Addresses = () => {
    const { user } = useUserStore();

    if (!user) {
        return (
            <div className="container mx-auto px-4 py-8">
                <div className="text-center">
                    <h1 className="text-2xl font-bold text-foreground mb-4">Acceso Denegado</h1>
                    <p className="text-muted-foreground">Debes iniciar sesión para gestionar tus direcciones.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-4 py-8">
            <div className="max-w-4xl mx-auto">
                <h1 className="text-3xl font-bold text-foreground mb-8">Mis Direcciones</h1>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    <div>
                        <AddressManager
                            type="shipping"
                            title="Direcciones de Envío"
                        />
                    </div>

                    <div>
                        <AddressManager
                            type="billing"
                            title="Direcciones de Facturación"
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Addresses;

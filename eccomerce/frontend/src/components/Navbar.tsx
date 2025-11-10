import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect, useRef } from 'react';
import { useUserStore } from '../stores/userStore';
import { useCartStore } from '../stores/cartStore';
import { profileAPI } from '../services/api';
import { ShoppingCart, Search, User, Menu, X, Sun, Moon } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';

const Navbar = () => {
  // console.log('Barra de navegación: Componente renderizado');
  const { user, logout } = useUserStore();
  const { items, loadCart } = useCartStore();
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Load cart when user logs in
  useEffect(() => {
    if (user) {
      // console.log('Barra de navegación: Usuario logueado, cargando carrito');
      loadCart();
    }
  }, [user, loadCart]);

  // Cerrar dropdown al hacer click fuera
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleDeactivateAccount = async () => {
    if (window.confirm('¿Estás seguro de que quieres desactivar tu cuenta? Esta acción no se puede deshacer.')) {
      try {
        await profileAPI.deactivateAccount();
        logout();
        // Account deactivated successfully
      } catch (error) {
        // Error handling removed for production mode
        // Error deactivating account
      }
    }
  };

  const handleSearch = () => {
    // console.log('Navbar: handleSearch llamado con:', searchTerm);
    if (searchTerm.trim()) {
      // console.log('Navbar: Navegando a página de búsqueda con:', searchTerm);
      navigate(`/search?q=${encodeURIComponent(searchTerm)}`);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    // console.log('Navbar: handleKeyDown llamado con tecla:', e.key);
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);

  // console.log('Barra de navegación: Estado - usuario:', user, 'items del carrito:', items.length);
  // console.log('Barra de navegación: ¿usuario es null?', user === null);
  // console.log('Barra de navegación: tipo de usuario:', typeof user);
  // console.log('Barra de navegación: nombre del usuario:', user?.name);
  // console.log('Barra de navegación: email del usuario:', user?.email);

  return (
    <>
      <header className="sticky top-0 z-50 w-full border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto px-4">
          <div className="flex h-16 items-center justify-between">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-2">
              <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center">
                <span className="text-primary-foreground font-bold text-lg">E</span>
              </div>
              <span className="font-bold text-xl text-foreground">EcomStore</span>
            </Link>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center space-x-8">
              <Link to="/products" className="text-muted-foreground hover:text-foreground transition-colors">
                Productos
              </Link>
              <Link to="/categories" className="text-muted-foreground hover:text-foreground transition-colors">
                Categorías
              </Link>
              <Link to="/about" className="text-muted-foreground hover:text-foreground transition-colors">
                About
              </Link>
            </nav>

            {/* Search Bar */}
            <div className="hidden md:flex items-center space-x-4 flex-1 max-w-md mx-8">
              <div className="relative w-full">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                <input
                  type="text"
                  placeholder="Search products..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={handleKeyDown}
                  className="w-full pl-10 pr-4 py-2 bg-muted/50 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center space-x-4">
              {/* Theme Toggle */}
              <button
                onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
                className="text-foreground hover:text-muted-foreground transition-colors"
                title={theme === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
              >
                {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
              </button>
              {/* User Menu */}
              {user ? (
                <div className="relative" ref={dropdownRef}>
                  <button
                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                    className="text-foreground hover:text-muted-foreground transition-colors flex items-center space-x-2"
                  >
                    <User className="h-4 w-4" />
                    <span className="hidden md:inline">{user.name || 'User'}</span>
                  </button>

                  {isDropdownOpen && (
                    <div className="absolute right-0 mt-2 w-48 bg-background rounded-md shadow-lg border border-border z-50">
                      <div className="py-1">
                        <Link
                          to="/profile"
                          className="block px-4 py-2 text-sm text-foreground hover:bg-muted transition-colors"
                          onClick={() => setIsDropdownOpen(false)}
                        >
                          Ver Perfil
                        </Link>
                        <Link
                          to="/profile/edit"
                          className="block px-4 py-2 text-sm text-foreground hover:bg-muted transition-colors"
                          onClick={() => setIsDropdownOpen(false)}
                        >
                          ✏️ Editar Perfil
                        </Link>
                        <Link
                          to="/addresses"
                          className="block px-4 py-2 text-sm text-foreground hover:bg-muted transition-colors"
                          onClick={() => setIsDropdownOpen(false)}
                        >
                          Mis Direcciones
                        </Link>
                        <Link
                          to="/orders"
                          className="block px-4 py-2 text-sm text-foreground hover:bg-muted transition-colors"
                          onClick={() => setIsDropdownOpen(false)}
                        >
                          Mis Compras
                        </Link>
                        {user.role === 'ADMIN' && (
                          <Link
                            to="/admin"
                            className="block px-4 py-2 text-sm text-foreground hover:bg-muted transition-colors"
                            onClick={() => setIsDropdownOpen(false)}
                          >
                            ⚙️ Panel de Admin
                          </Link>
                        )}
                        <div className="border-t border-border"></div>
                        <button
                          onClick={() => {
                            handleDeactivateAccount();
                            setIsDropdownOpen(false);
                          }}
                          className="block w-full text-left px-4 py-2 text-sm text-destructive hover:bg-muted transition-colors"
                        >
                          ️ Borrar Cuenta
                        </button>
                        <button
                          onClick={() => {
                            // console.log('Barra de navegación: Botón de cerrar sesión clickeado');
                            logout();
                            setIsDropdownOpen(false);
                            // console.log('Barra de navegación: Usuario desconectado, debería redirigir al inicio');
                          }}
                          className="block w-full text-left px-4 py-2 text-sm text-destructive hover:bg-muted transition-colors"
                        >
                          Cerrar Sesión
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <Link to="/login">
                  <button className="text-muted-foreground hover:text-foreground transition-colors flex items-center space-x-2">
                    <User className="h-4 w-4" />
                    <span className="hidden md:inline">Login</span>
                  </button>
                </Link>
              )}

              {/* Cart */}
              <Link to="/cart" className="relative text-foreground hover:text-muted-foreground transition-colors">
                <ShoppingCart className="h-5 w-5" />
                {itemCount > 0 && (
                  <span className="absolute -top-2 -right-2 bg-destructive text-destructive-foreground text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center">
                    {itemCount}
                  </span>
                )}
              </Link>

              {/* Mobile Menu Toggle */}
              <button
                className="md:hidden text-foreground hover:text-muted-foreground transition-colors"
                onClick={() => setIsMenuOpen(!isMenuOpen)}
              >
                {isMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
              </button>
            </div>
          </div>

          {/* Mobile Menu */}
          {isMenuOpen && (
            <div className="md:hidden border-t border-border py-4">
              <nav className="flex flex-col space-y-4">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                  <input
                    type="text"
                    placeholder="Search products..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyDown={handleKeyDown}
                    className="w-full pl-10 pr-4 py-2 bg-muted/50 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring"
                  />
                </div>
                <Link to="/products" className="text-muted-foreground hover:text-foreground transition-colors">
                  Productos
                </Link>
                <Link to="/categories" className="text-muted-foreground hover:text-foreground transition-colors">
                  Categorías
                </Link>
                <Link to="/about" className="text-muted-foreground hover:text-foreground transition-colors">
                  About
                </Link>
                {!user && (
                  <Link to="/login" className="text-muted-foreground hover:text-foreground transition-colors">
                    Login
                  </Link>
                )}
                <button
                  onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
                  className="text-muted-foreground hover:text-foreground transition-colors flex items-center space-x-2"
                  title={theme === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
                >
                  {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                  <span>{theme === 'dark' ? 'Tema Claro' : 'Tema Oscuro'}</span>
                </button>
              </nav>
            </div>
          )}
        </div>
      </header>
    </>
  );
  // console.log('Barra de navegación: Renderizado del componente completado');
};

export default Navbar;

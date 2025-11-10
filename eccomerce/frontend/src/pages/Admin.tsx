import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUserStore } from '../stores/userStore';
import { useNotificationStore } from '../stores/notificationStore';
import { adminProductAPI, adminUserAPI, adminImageAPI, auditAPI, adminOrderAPI, paymentAPI, adminVariantAttributeAPI, adminCategoryAPI, adminReviewAPI } from '../services/api';
import { useTheme } from '../contexts/ThemeContext';
import Pagination from '../components/Pagination';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  RadialLinearScale
} from 'chart.js';
import { Line, Bar, Doughnut, Pie } from 'react-chartjs-2';

// Registrar componentes de ChartJS
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  RadialLinearScale
);

interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  stockQuantity?: number;
  sku?: string;
  categoryId?: number;
  category?: Category;
  active?: boolean;
  featured?: boolean;
}

interface ProductUpdateRequest {
  name: string;
  description?: string;
  shortDescription?: string;
  price: number;
  compareAtPrice?: number;
  stockQuantity: number;
  lowStockThreshold?: number;
  sku?: string;
  barcode?: string;
  categoryId?: number;
  active?: boolean;
  featured?: boolean;
  imageUrl?: string;
  seoTitle?: string;
  seoDescription?: string;
  seoKeywords?: string;
}

interface Order {
  id: number;
  total: number;
  status: string;
  createdAt: string;
  user?: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
  };
  items?: OrderItem[];
  shippingAddress?: string;
}

interface OrderItem {
  id: number;
  product: Product;
  quantity: number;
  price: number;
}

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
  createdAt: string;
}

interface Category {
  id: number | string;
  name: string;
  description?: string;
  sortOrder?: number;
  active?: boolean;
  parentId?: number | null;
  subcategories?: Category[];
  products?: Product[];
}

interface CategoryRequest {
  name: string;
  description?: string;
  parentId?: number | null;
  sortOrder?: number;
  active?: boolean;
}

interface DashboardStats {
  totalProducts: number;
  totalOrders: number;
  totalUsers: number;
  totalRevenue: number;
  recentOrders: Order[];
  lowStockProducts: Product[];
  salesByMonth?: { month: string; revenue: number }[];
  ordersByStatus?: { status: string; count: number }[];
  productsByCategory?: { category: string; count: number }[];
  userRegistrationsByMonth?: { month: string; count: number }[];
}

interface AuditLog {
  id: number;
  userId?: number;
  userEmail?: string;
  action: string;
  entityType: string;
  entityId: string;
  oldValues?: string;
  newValues?: string;
  details?: string;
  ipAddress?: string;
  userAgent?: string;
  sessionId?: string;
  success: boolean;
  errorMessage?: string;
  executionTimeMs?: number;
  requestMethod?: string;
  requestUrl?: string;
  timestamp: string;
}

interface AuditFilters {
  userEmail?: string;
  action?: string;
  entityType?: string;
  entityId?: string;
  success?: boolean;
  startDate?: string;
  endDate?: string;
}

interface VariantAttribute {
  id: number;
  name: string;
  displayName: string;
  description?: string;
  attributeType: 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN';
  unit?: string;
  // options?: string[];
  required: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface VariantAttributeRequest {
  name: string;
  displayName: string;
  description?: string;
  attributeType: 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN';
  unit?: string;
  // options?: string[];
  required: boolean;
}

interface VariantAttribute {
  id: number;
  name: string;
  displayName: string;
  description?: string;
  attributeType: 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN';
  unit?: string;
  options?: string[];
  required: boolean;
  active: boolean;
  global: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

interface VariantAttributeRequest {
  name: string;
  displayName: string;
  description?: string;
  attributeType: 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN';
  unit?: string;
  options?: string[];
  required: boolean;
  global: boolean;
  categoryIds?: number[]; // Para atributos específicos por categoría
}

// Funciones auxiliares para generar datos de gráficos
const generateSalesByMonthData = (orders: Order[]) => {
  const months = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

  // Obtener los últimos 6 meses
  const today = new Date();
  const salesData: { month: string; revenue: number }[] = [];

  for (let i = 5; i >= 0; i--) {
    const month = new Date(today.getFullYear(), today.getMonth() - i, 1);
    const monthName = months[month.getMonth()];
    // const monthYear = `${monthName} ${month.getFullYear()}`; // No se usa actualmente

    // Filtrar órdenes del mes actual
    const monthOrders = orders.filter(order => {
      const orderDate = new Date(order.createdAt);
      return orderDate.getMonth() === month.getMonth() &&
        orderDate.getFullYear() === month.getFullYear();
    });

    // Calcular ingresos
    const revenue = monthOrders.reduce((sum, order) => sum + order.total, 0);

    salesData.push({ month: monthName, revenue });
  }

  return salesData;
};

const generateOrdersByStatusData = (orders: Order[]) => {
  // Contar pedidos por estado
  const statusCounts: Record<string, number> = {};

  orders.forEach(order => {
    const status = order.status || 'Desconocido';
    statusCounts[status] = (statusCounts[status] || 0) + 1;
  });

  // Convertir a formato para el gráfico
  return Object.entries(statusCounts).map(([status, count]) => ({
    status,
    count
  }));
};

const generateProductsByCategoryData = (products: Product[]) => {
  // Contar productos por categoría
  const categoryCounts: Record<string, number> = {};

  products.forEach(product => {
    const category = product.category?.name || 'Sin categoría';
    categoryCounts[category] = (categoryCounts[category] || 0) + 1;
  });

  // Convertir a formato para el gráfico
  return Object.entries(categoryCounts).map(([category, count]) => ({
    category,
    count
  }));
};

const generateUserRegistrationsData = (users: User[]) => {
  const months = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

  // Obtener los últimos 6 meses
  const today = new Date();
  const registrationData: { month: string; count: number }[] = [];

  for (let i = 5; i >= 0; i--) {
    const month = new Date(today.getFullYear(), today.getMonth() - i, 1);
    const monthName = months[month.getMonth()];

    // Filtrar usuarios registrados en el mes actual
    const monthUsers = users.filter(user => {
      const regDate = new Date(user.createdAt);
      return regDate.getMonth() === month.getMonth() &&
        regDate.getFullYear() === month.getFullYear();
    });

    registrationData.push({ month: monthName, count: monthUsers.length });
  }

  return registrationData;
};

const Admin = () => {
  const { user } = useUserStore();
  const { theme, setTheme } = useTheme();
  const { addNotification } = useNotificationStore();
  const navigate = useNavigate();

  // State management
  const [activeTab, setActiveTab] = useState('dashboard');
  const [dashboardStats, setDashboardStats] = useState<DashboardStats | null>(null);
  const [orders, setOrders] = useState<Order[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [userPage, setUserPage] = useState(0);
  const [userTotalPages, setUserTotalPages] = useState(0);
  const [userTotalElements, setUserTotalElements] = useState(0);
  const [userSize] = useState(10);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditFilters, setAuditFilters] = useState<AuditFilters>({});
  const [auditSortBy, setAuditSortBy] = useState<string>('timestamp');
  const [auditSortDir, setAuditSortDir] = useState<'asc' | 'desc'>('desc');
  const [auditPage, setAuditPage] = useState(0);
  const [auditTotalPages, setAuditTotalPages] = useState(0);
  const [adminProducts, setAdminProducts] = useState<Product[]>([]);
  const [adminProductsLoading, setAdminProductsLoading] = useState(false);
  const [productPage, setProductPage] = useState(0);
  const [productTotalPages, setProductTotalPages] = useState(0);
  const [productTotalElements, setProductTotalElements] = useState(0);
  const [productSize] = useState(10);
  const [checkingPendingPayments, setCheckingPendingPayments] = useState(false);

  // Variant attributes state
  const [variantAttributes, setVariantAttributes] = useState<VariantAttribute[]>([]);
  const [variantAttributesLoading, setVariantAttributesLoading] = useState(false);
  const [showVariantAttributeModal, setShowVariantAttributeModal] = useState(false);
  const [editingVariantAttribute, setEditingVariantAttribute] = useState<VariantAttribute | null>(null);
  const [newVariantAttribute, setNewVariantAttribute] = useState<VariantAttributeRequest>({
    name: '',
    displayName: '',
    description: '',
    attributeType: 'TEXT',
    unit: '',
    options: [],
    required: false,
    global: true, // Por defecto global
    categoryIds: []
  });
  const [selectedCategories, setSelectedCategories] = useState<number[]>([]);

  // Categories state
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoriesLoading, setCategoriesLoading] = useState(false);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [newCategory, setNewCategory] = useState({
    name: '',
    description: '',
    parentId: null as number | null,
    sortOrder: 0,
    active: true
  });

  // Product management state
  const [showEditProductModal, setShowEditProductModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [editingProductId, setEditingProductId] = useState<number | null>(null);
  const [newProduct, setNewProduct] = useState({
    name: '',
    description: '',
    shortDescription: '',
    price: 0,
    compareAtPrice: 0,
    stockQuantity: 0,
    lowStockThreshold: 5,
    sku: '',
    barcode: '',
    categoryId: null as number | null,
    imageUrl: '',
    active: true,
    featured: false,
    seoTitle: '',
    seoDescription: '',
    seoKeywords: ''
  });
  const [uploadingImage, setUploadingImage] = useState(false);

  // Hard delete state
  const [showHardDeleteModal, setShowHardDeleteModal] = useState(false);
  const [hardDeleteTarget, setHardDeleteTarget] = useState<{ id: number, name: string, type: 'product' | 'user' } | null>(null);
  const [hardDeletePassword, setHardDeletePassword] = useState('');
  const [hardDeleteLoading, setHardDeleteLoading] = useState(false);

  // Order detail state
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [showOrderDetailModal, setShowOrderDetailModal] = useState(false);

  // User role change state
  const [showRoleChangeModal, setShowRoleChangeModal] = useState(false);
  const [roleChangeTarget, setRoleChangeTarget] = useState<{ id: number, name: string, currentRole: string } | null>(null);
  const [newRole, setNewRole] = useState('');
  const [roleChangeLoading, setRoleChangeLoading] = useState(false);

  // Review management state
  const [reviews, setReviews] = useState<any[]>([]);
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [reviewTotalElements, setReviewTotalElements] = useState(0);
  const [reviewSize] = useState(10);
  const [reviewsLoading, setReviewsLoading] = useState(false);

  // Review hide modal state
  const [showHideReviewModal, setShowHideReviewModal] = useState(false);
  const [reviewToHide, setReviewToHide] = useState<{ id: number; productName: string } | null>(null);
  const [hideReviewReason, setHideReviewReason] = useState('');
  const [hidingReview, setHidingReview] = useState(false);

  const tabs = [
    { id: 'dashboard', label: 'Dashboard', icon: '' },
    { id: 'products', label: 'Productos', icon: '' },
    { id: 'categories', label: 'Categorías', icon: '' },
    { id: 'variants', label: 'Atributos de Variantes', icon: '️' },
    { id: 'orders', label: 'Pedidos', icon: '' },
    { id: 'reviews', label: 'Reseñas', icon: '' },
    { id: 'users', label: 'Usuarios', icon: '' },
    { id: 'audit', label: 'Auditoría', icon: '' }
  ];

  // Data fetching functions
  const fetchAdminProducts = async (page: number = 0, size: number = 10) => {
    // console.log('Admin: Cargando productos de admin');
    setAdminProductsLoading(true);
    try {
      const response = await adminProductAPI.getProducts(page, size);
      // console.log('✅ Admin: Productos de admin obtenidos:', response.data);
      const data = response.data;
      setAdminProducts(data.content || data || []);
      setProductPage(data.pageable?.pageNumber || page);
      setProductTotalPages(data.totalPages || 0);
      setProductTotalElements(data.totalElements || 0);
      // console.log('Admin: Productos de admin establecidos:', data.content?.length || 0, 'productos');
    } catch (error) {
      // Error handling in production mode
    } finally {
      setAdminProductsLoading(false);
    }
  };

  // Categories functions
  const fetchCategories = async () => {
    setCategoriesLoading(true);
    try {
      const response = await adminCategoryAPI.getCategories();
      setCategories(response.data || []);
    } catch (error) {
      // Error handling in production mode
    } finally {
      setCategoriesLoading(false);
    }
  };

  const handleCreateCategory = async () => {
    const categoryData: CategoryRequest = {
      name: newCategory.name,
      description: newCategory.description,
      parentId: newCategory.parentId,
      sortOrder: newCategory.sortOrder,
      active: newCategory.active
    };
    try {
      await adminCategoryAPI.createCategory(categoryData);
      setShowCategoryModal(false);
      setNewCategory({ name: '', description: '', parentId: null, sortOrder: 0, active: true });
      fetchCategories();
    } catch (error) {
      // Error handling in production mode
    }
  };

  const handleUpdateCategory = async () => {
    if (!editingCategory) return;
    try {
      await adminCategoryAPI.updateCategory(Number(editingCategory.id), editingCategory);
      setShowCategoryModal(false);
      setEditingCategory(null);
      fetchCategories();
    } catch (error) {
      // Error handling in production mode
    }
  };

  const handleDeleteCategory = async (id: number) => {
    if (!confirm('¿Estás seguro de que quieres eliminar esta categoría?')) return;
    try {
      await adminCategoryAPI.deleteCategory(id);
      fetchCategories();
    } catch (error) {
      // Error handling in production mode
    }
  };

  const handleRestoreCategory = async (id: number) => {
    try {
      await adminCategoryAPI.restoreCategory(id);
      fetchCategories();
    } catch (error) {
      // Error handling in production mode
    }
  };

  const openEditCategoryModal = (category: Category) => {
    setEditingCategory(category);
    setShowCategoryModal(true);
  };

  const openCreateCategoryModal = () => {
    setEditingCategory(null);
    setNewCategory({ name: '', description: '', parentId: null, sortOrder: 0, active: true });
    setShowCategoryModal(true);
  };

  const fetchDashboardStats = async () => {
    try {
      const [productsRes, ordersRes, usersRes] = await Promise.all([
        adminProductAPI.getProducts(),
        adminOrderAPI.getOrders(),
        adminUserAPI.getUsers(0, 1000) // Get all users for dashboard stats
      ]);

      const products = productsRes.data.content || [];
      const orders = ordersRes.data || [];
      const users = usersRes.data.content || usersRes.data || [];

      const totalRevenue = orders.reduce((sum: number, order: Order) => sum + order.total, 0);
      const lowStockProducts = products.filter((p: Product) => (p.stockQuantity || 0) < 10);
      const recentOrders = orders.slice(0, 5);

      // Datos para gráficos
      // Datos de ventas por mes (últimos 6 meses)
      const salesByMonth = generateSalesByMonthData(orders);

      // Datos de pedidos por estado
      const ordersByStatus = generateOrdersByStatusData(orders);

      // Datos de productos por categoría
      const productsByCategory = generateProductsByCategoryData(products);

      // Datos de registro de usuarios por mes (últimos 6 meses)
      const userRegistrationsByMonth = generateUserRegistrationsData(users);

      setDashboardStats({
        totalProducts: productsRes.data.totalElements || products.length || 0,
        totalOrders: orders.length,
        totalUsers: users.length || usersRes.data.totalElements || 0,
        totalRevenue,
        recentOrders,
        lowStockProducts,
        salesByMonth,
        ordersByStatus,
        productsByCategory,
        userRegistrationsByMonth
      });
    } catch (error) {
      // Error handling in production mode
    }
  };

  const fetchAllOrders = async () => {
    try {
      const response = await adminOrderAPI.getOrders();
      setOrders(response.data);
    } catch (error) {
      // Error handling in production mode
    }
  };

  const fetchAllUsers = async (page: number = 0, size: number = 10) => {
    try {
      const response = await adminUserAPI.getUsers(page, size);
      const data = response.data;
      setUsers(data.content || data);
      setUserPage(data.pageable?.pageNumber || page);
      setUserTotalPages(data.totalPages || 0);
      setUserTotalElements(data.totalElements || 0);
    } catch (error) {
      // Error handling in production mode
    }
  };

  const fetchAuditLogs = async () => {
    try {
      setAuditLoading(true);
      // console.log('Fetching audit logs with filters:', auditFilters);
      // console.log('Current audit state:', {
      //   page: auditPage,
      //   sortBy: auditSortBy,
      //   sortDir: auditSortDir,
      //   activeTab,
      //   userRole: user?.role
      // });
      // console.log('Filters being sent to API:', {
      //   sortBy: auditSortBy,
      //   sortDir: auditSortDir,
      //   ...auditFilters
      // });

      const filtersToSend: any = {
        sortBy: auditSortBy,
        sortDir: auditSortDir,
      };

      // Solo incluir filtros que tengan valores reales
      if (auditFilters.userEmail && auditFilters.userEmail.trim()) {
        filtersToSend.userEmail = auditFilters.userEmail.trim();
      }
      if (auditFilters.action) {
        filtersToSend.action = auditFilters.action;
      }
      if (auditFilters.entityType) {
        filtersToSend.entityType = auditFilters.entityType;
      }
      if (auditFilters.entityId && auditFilters.entityId.trim()) {
        filtersToSend.entityId = auditFilters.entityId.trim();
      }
      if (auditFilters.success !== undefined) {
        filtersToSend.success = auditFilters.success;
      }
      if (auditFilters.startDate) {
        // Enviar fecha en formato ISO sin timezone para mejor compatibilidad
        const date = new Date(auditFilters.startDate.split('T')[0] + 'T00:00:00');
        filtersToSend.startDate = date.toISOString();
      }
      if (auditFilters.endDate) {
        // Enviar fecha en formato ISO sin timezone para mejor compatibilidad
        const date = new Date(auditFilters.endDate.split('T')[0] + 'T23:59:59');
        filtersToSend.endDate = date.toISOString();
      }

      // console.log('Filters being sent to API:', filtersToSend);

      const response = await auditAPI.getAuditLogs(auditPage, 20, filtersToSend);

      // console.log('✅ Audit logs response:', response);
      // console.log('✅ Response data:', response.data);
      // console.log('✅ Response data content:', response.data.content);
      // console.log('✅ Response data totalPages:', response.data.totalPages);

      const logsData = response.data.content || response.data;
      // console.log('Setting audit logs:', logsData);
      // console.log('Logs count:', logsData.length);

      setAuditLogs(logsData);
      setAuditTotalPages(response.data.totalPages || 1);
    } catch (error) {
      // Error handling removed for production mode
      setAuditLogs([]);
    } finally {
      setAuditLoading(false);
    }
  };

  const formatAuditAction = (action: string): string => {
    const actionMap: { [key: string]: string } = {
      'LOGIN': 'Inicio de sesión',
      'LOGOUT': 'Cierre de sesión',
      'LOGIN_FAILED': 'Login fallido',
      'PASSWORD_RESET': 'Restablecer contraseña',
      'PASSWORD_CHANGE': 'Cambiar contraseña',
      'USER_CREATE': 'Crear usuario',
      'USER_UPDATE': 'Actualizar usuario',
      'USER_DELETE': 'Eliminar usuario',
      'USER_ACTIVATE': 'Activar usuario',
      'USER_DEACTIVATE': 'Desactivar usuario',
      'USER_ROLE_CHANGE': 'Cambiar rol',
      'PRODUCT_CREATE': 'Crear producto',
      'PRODUCT_UPDATE': 'Actualizar producto',
      'PRODUCT_DELETE': 'Eliminar producto',
      'PRODUCT_STOCK_UPDATE': 'Actualizar stock',
      'CATEGORY_CREATE': 'Crear categoría',
      'CATEGORY_UPDATE': 'Actualizar categoría',
      'CATEGORY_DELETE': 'Eliminar categoría',
      'ORDER_CREATE': 'Crear pedido',
      'ORDER_UPDATE': 'Actualizar pedido',
      'ORDER_CANCEL': 'Cancelar pedido',
      'ORDER_COMPLETE': 'Completar pedido',
      'ORDER_PAYMENT': 'Pagar pedido',
      'CART_ADD_ITEM': 'Agregar al carrito',
      'CART_REMOVE_ITEM': 'Remover del carrito',
      'CART_UPDATE_QUANTITY': 'Actualizar cantidad',
      'CART_CLEAR': 'Vaciar carrito',
      'REVIEW_CREATE': 'Crear reseña',
      'REVIEW_UPDATE': 'Actualizar reseña',
      'REVIEW_DELETE': 'Eliminar reseña',
      'HTTP_REQUEST': 'Petición HTTP',
      'SYSTEM_BACKUP': 'Respaldo sistema',
      'SYSTEM_MAINTENANCE': 'Mantenimiento',
      'CONFIG_UPDATE': 'Actualizar configuración'
    };
    return actionMap[action] || action;
  };

  const formatEntityType = (entityType: string): string => {
    const entityMap: { [key: string]: string } = {
      'User': 'Usuario',
      'Product': 'Producto',
      'Category': 'Categoría',
      'Order': 'Pedido',
      'Payment': 'Pago',
      'Review': 'Reseña',
      'Cart': 'Carrito',
      'HTTP': 'HTTP'
    };
    return entityMap[entityType] || entityType;
  };

  const handleAuditSort = (column: string) => {
    if (auditSortBy === column) {
      setAuditSortDir(auditSortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setAuditSortBy(column);
      setAuditSortDir('desc');
    }
    setAuditPage(0);
  };

  const handleAuditFilterChange = (key: keyof AuditFilters, value: string | boolean | undefined) => {
    setAuditFilters(prev => ({ ...prev, [key]: value }));
    setAuditPage(0);
  };

  const clearAuditFilters = () => {
    setAuditFilters({});
    setAuditPage(0);
  };

  // Image upload function
  const handleImageUpload = async (file: File, isEdit: boolean = false) => {
    setUploadingImage(true);
    try {
      const response = await adminImageAPI.uploadImage(file);
      const imageUrl = response;

      if (isEdit && editingProduct) {
        setEditingProduct({ ...editingProduct, imageUrl });
      } else {
        setNewProduct({ ...newProduct, imageUrl });
      }
    } catch (error) {
      // Error handling removed for production mode
      // Error is logged, user will see the error in the UI
    } finally {
      setUploadingImage(false);
    }
  };

  // Product management functions
  const handleEditProduct = (product: Product) => {
    setEditingProduct(product);
    setEditingProductId(product.id);
    setShowEditProductModal(true);
  };

  const handleAddProduct = () => {
    setEditingProduct(null);
    setEditingProductId(null);
    setNewProduct({
      name: '',
      description: '',
      shortDescription: '',
      price: 0,
      compareAtPrice: 0,
      stockQuantity: 0,
      lowStockThreshold: 5,
      sku: '',
      barcode: '',
      categoryId: null,
      imageUrl: '',
      active: true,
      featured: false,
      seoTitle: '',
      seoDescription: '',
      seoKeywords: ''
    });
    setShowEditProductModal(true);
  };

  const handleCreateProduct = async () => {
    if (!newProduct.name.trim() || newProduct.price <= 0 || newProduct.stockQuantity < 0) {
      // Error is logged, user will see the error in the UI
      return;
    }

    try {
      const productData = {
        name: newProduct.name.trim(),
        description: newProduct.description?.trim() || null,
        shortDescription: newProduct.shortDescription?.trim() || null,
        price: Number(newProduct.price),
        compareAtPrice: newProduct.compareAtPrice > 0 ? Number(newProduct.compareAtPrice) : null,
        stockQuantity: Number(newProduct.stockQuantity),
        lowStockThreshold: Number(newProduct.lowStockThreshold),
        sku: newProduct.sku?.trim() || null,
        barcode: newProduct.barcode?.trim() || null,
        categoryId: newProduct.categoryId,
        imageUrl: newProduct.imageUrl?.trim() || null,
        active: Boolean(newProduct.active),
        featured: Boolean(newProduct.featured),
        seoTitle: newProduct.seoTitle?.trim() || null,
        seoDescription: newProduct.seoDescription?.trim() || null,
        seoKeywords: newProduct.seoKeywords?.trim() || null
      };

      await adminProductAPI.createProduct(productData);

      // Reset form and close modal
      setNewProduct({
        name: '',
        description: '',
        shortDescription: '',
        price: 0,
        compareAtPrice: 0,
        stockQuantity: 0,
        lowStockThreshold: 5,
        sku: '',
        barcode: '',
        categoryId: null,
        imageUrl: '',
        active: true,
        featured: false,
        seoTitle: '',
        seoDescription: '',
        seoKeywords: ''
      });
      setShowEditProductModal(false);

      // Refresh products list
      fetchAdminProducts();

      addNotification({
        type: 'success',
        message: 'Producto creado exitosamente',
        duration: 3000
      });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Error al crear el producto';

      addNotification({
        type: 'error',
        message: errorMessage,
        duration: 5000
      });
    }
  };

  const handleUpdateProduct = async () => {
    if (!editingProduct || !editingProductId) return;

    try {
      const productData: ProductUpdateRequest = {
        name: editingProduct.name.trim(),
        price: Number(editingProduct.price),
        stockQuantity: Number(editingProduct.stockQuantity),
        lowStockThreshold: Number((editingProduct as any).lowStockThreshold || 5),
        active: Boolean(editingProduct.active),
        featured: Boolean(editingProduct.featured)
      };

      if (editingProduct.description?.trim()) {
        productData.description = editingProduct.description.trim();
      }
      if ((editingProduct as any).shortDescription?.trim()) {
        productData.shortDescription = (editingProduct as any).shortDescription.trim();
      }
      if ((editingProduct as any).compareAtPrice > 0) {
        productData.compareAtPrice = Number((editingProduct as any).compareAtPrice);
      }
      if (editingProduct.sku) {
        productData.sku = editingProduct.sku;
      }
      if ((editingProduct as any).barcode?.trim()) {
        productData.barcode = (editingProduct as any).barcode.trim();
      }
      if (editingProduct.categoryId !== null) {
        productData.categoryId = editingProduct.categoryId;
      }
      if (editingProduct.imageUrl?.trim()) {
        productData.imageUrl = editingProduct.imageUrl.trim();
      }
      if ((editingProduct as any).seoTitle?.trim()) {
        productData.seoTitle = (editingProduct as any).seoTitle.trim();
      }
      if ((editingProduct as any).seoDescription?.trim()) {
        productData.seoDescription = (editingProduct as any).seoDescription.trim();
      }
      if ((editingProduct as any).seoKeywords?.trim()) {
        productData.seoKeywords = (editingProduct as any).seoKeywords.trim();
      }

      await adminProductAPI.updateProduct(editingProductId, productData);
      setEditingProduct(null);
      setEditingProductId(null);
      setShowEditProductModal(false);
      await fetchAdminProducts();
      await fetchDashboardStats();

      addNotification({
        type: 'success',
        message: 'Producto actualizado exitosamente',
        duration: 3000
      });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Error al actualizar el producto';

      addNotification({
        type: 'error',
        message: errorMessage,
        duration: 5000
      });
    }
  };

  const handleSoftDeleteProduct = async (productId: number) => {
    if (confirm('¿Estás seguro de que deseas desactivar este producto?')) {
      try {
        await adminProductAPI.softDeleteProduct(productId);
        await fetchAdminProducts();
        await fetchDashboardStats();
      } catch (error) {
        // Error handling removed for production mode
      }
    }
  };

  const handleHardDeleteProduct = async (productId: number, productName: string) => {
    setHardDeleteTarget({ id: productId, name: productName, type: 'product' });
    setShowHardDeleteModal(true);
  };

  const confirmHardDelete = async () => {
    if (!hardDeleteTarget) return;

    setHardDeleteLoading(true);
    try {
      if (hardDeleteTarget.type === 'product') {
        await adminProductAPI.hardDeleteProduct(hardDeleteTarget.id, hardDeletePassword);
        await fetchAdminProducts();
      } else {
        await adminUserAPI.hardDeleteUser(hardDeleteTarget.id, hardDeletePassword);
        await fetchAllUsers();
      }

      setHardDeleteTarget(null);
      setHardDeletePassword('');
      setShowHardDeleteModal(false);
      await fetchDashboardStats();
    } catch (error) {
      // Error handling removed for production mode
      // Error is logged, user will see the error in the UI
    } finally {
      setHardDeleteLoading(false);
    }
  };

  const handleSoftDeleteUser = async (userId: number) => {
    if (confirm('¿Estás seguro de que deseas desactivar este usuario?')) {
      try {
        await adminUserAPI.softDeleteUser(userId);
        await fetchAllUsers();
        await fetchDashboardStats();
      } catch (error) {
        // Error handling removed for production mode
      }
    }
  };

  const handleHardDeleteUser = async (userId: number, userName: string) => {
    setHardDeleteTarget({ id: userId, name: userName, type: 'user' });
    setShowHardDeleteModal(true);
  };

  const handleChangeUserRole = async (userId: number, userName: string, currentRole: string) => {
    setRoleChangeTarget({ id: userId, name: userName, currentRole });
    setNewRole(currentRole);
    setShowRoleChangeModal(true);
  };

  const handleViewOrderDetails = (order: Order) => {
    setSelectedOrder(order);
    setShowOrderDetailModal(true);
  };

  const handleConfirmRoleChange = async () => {
    if (!roleChangeTarget || !newRole) return;

    setRoleChangeLoading(true);
    try {
      await adminUserAPI.changeUserRole(roleChangeTarget.id, newRole);
      await fetchAllUsers();
      await fetchDashboardStats();
      setShowRoleChangeModal(false);
      setRoleChangeTarget(null);
      setNewRole('');
    } catch (error) {
      // Error handling removed for production mode
      // Error is logged, user will see the error in the UI
    } finally {
      setRoleChangeLoading(false);
    }
  };

  const handleCheckPendingPayments = async () => {
    setCheckingPendingPayments(true);
    try {
      const response = await paymentAPI.checkPendingPayments();
      // console.log('✅ Verificación de pagos pendientes completada:', response.data);
      alert(`Verificación completada. ${response.data.message || 'Pagos pendientes verificados.'}`);
      // Recargar los pedidos para ver los cambios
      await fetchAllOrders();
      await fetchDashboardStats();
    } catch (error) {
      // Error handling removed for production mode
      alert('Error al verificar pagos pendientes. Revisa la consola para más detalles.');
    } finally {
      setCheckingPendingPayments(false);
    }
  };

  // Pagination handlers
  const handleUserPageChange = (page: number) => {
    fetchAllUsers(page, userSize);
  };

  const handleProductPageChange = (page: number) => {
    fetchAdminProducts(page, productSize);
  };

  const handleReviewPageChange = (page: number) => {
    fetchReviews(page, reviewSize);
  };

  // Review management functions
  const fetchReviews = async (page: number = 0, size: number = 10) => {
    setReviewsLoading(true);
    try {
      const response = await adminReviewAPI.getAllReviews(page, size);
      const data = response.data;
      setReviews(data.content || data || []);
      setReviewPage(data.pageable?.pageNumber || page);
      setReviewTotalPages(data.totalPages || 0);
      setReviewTotalElements(data.totalElements || 0);
    } catch (error) {
      useNotificationStore.getState().addNotification({
        type: 'error',
        message: 'Error al cargar reseñas'
      });
    } finally {
      setReviewsLoading(false);
    }
  };

  const handleDeleteReview = (reviewId: number, productName: string) => {
    setReviewToHide({ id: reviewId, productName });
    setHideReviewReason('');
    setShowHideReviewModal(true);
  };

  const handleHideReviewSubmit = async () => {
    if (!reviewToHide || !hideReviewReason.trim()) {
      useNotificationStore.getState().addNotification({
        type: 'error',
        message: 'Por favor, ingresa una razón'
      });
      return;
    }

    if (hideReviewReason.length < 5) {
      useNotificationStore.getState().addNotification({
        type: 'error',
        message: 'La razón debe tener al menos 5 caracteres'
      });
      return;
    }

    setHidingReview(true);
    try {
      await adminReviewAPI.deleteReview(reviewToHide.id, hideReviewReason);
      useNotificationStore.getState().addNotification({
        type: 'success',
        message: `Reseña de "${reviewToHide.productName}" ocultada exitosamente`
      });
      setShowHideReviewModal(false);
      setReviewToHide(null);
      setHideReviewReason('');
      // Recargar reseñas
      fetchReviews(reviewPage, reviewSize);
    } catch (error) {
      useNotificationStore.getState().addNotification({
        type: 'error',
        message: 'Error al ocultar reseña'
      });
    } finally {
      setHidingReview(false);
    }
  };

  // Render functions
  const renderDashboard = () => {
    // Función para obtener colores de CSS variables
    const getCssVar = (variable: string, opacity: number = 1) => {
      const value = getComputedStyle(document.documentElement).getPropertyValue(variable).trim();
      const hsl = value.split(' ');
      return `hsla(${hsl[0]}, ${hsl[1]}, ${hsl[2]}, ${opacity})`;
    };

    // Configuración para gráfico de ventas por mes
    const salesChartData = {
      labels: dashboardStats?.salesByMonth?.map(item => item.month) || [],
      datasets: [
        {
          label: 'Ventas ($)',
          data: dashboardStats?.salesByMonth?.map(item => item.revenue) || [],
          backgroundColor: getCssVar('--chart-blue', 0.2),
          borderColor: getCssVar('--chart-blue', 1),
          borderWidth: 2,
          tension: 0.4,
          fill: true,
        }
      ]
    };

    // Configuración para gráfico de pedidos por estado
    const orderStatusChartData = {
      labels: dashboardStats?.ordersByStatus?.map(item => item.status) || [],
      datasets: [
        {
          data: dashboardStats?.ordersByStatus?.map(item => item.count) || [],
          backgroundColor: [
            getCssVar('--chart-blue', 0.9),
            getCssVar('--chart-green', 0.9),
            getCssVar('--chart-yellow', 0.9),
            getCssVar('--chart-red', 0.9),
            getCssVar('--chart-purple', 0.9),
          ],
          borderWidth: 1,
        }
      ]
    };

    // Configuración para gráfico de productos por categoría
    const categoryChartData = {
      labels: dashboardStats?.productsByCategory?.map(item => item.category) || [],
      datasets: [
        {
          data: dashboardStats?.productsByCategory?.map(item => item.count) || [],
          backgroundColor: [
            getCssVar('--chart-red', 0.9),
            getCssVar('--chart-blue', 0.9),
            getCssVar('--chart-yellow', 0.9),
            getCssVar('--chart-green', 0.9),
            getCssVar('--chart-purple', 0.9),
            getCssVar('--chart-orange', 0.9),
            getCssVar('--chart-cyan', 0.9),
          ],
          borderWidth: 1,
        }
      ]
    };

    // Configuración para gráfico de registros de usuarios
    const userRegistrationsChartData = {
      labels: dashboardStats?.userRegistrationsByMonth?.map(item => item.month) || [],
      datasets: [
        {
          label: 'Nuevos usuarios',
          data: dashboardStats?.userRegistrationsByMonth?.map(item => item.count) || [],
          backgroundColor: getCssVar('--chart-purple', 0.7),
          borderColor: getCssVar('--chart-purple', 1),
          borderWidth: 2,
        }
      ]
    };

    // Opciones comunes para los gráficos
    const commonOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'top' as const,
        },
      },
    };

    return (
      <div className="space-y-8">
        {/* Tarjetas de estadísticas */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Total Productos</p>
                <p className="text-3xl font-bold text-foreground mt-2">{dashboardStats?.totalProducts || 0}</p>
                <p className="text-xs text-muted-foreground mt-1">Productos activos</p>
              </div>
              <div className="p-4 rounded-full bg-blue-100 dark:bg-blue-900">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-blue-600 dark:text-blue-400">
                  <path d="M20.91 8.84 8.56 2.23a1.93 1.93 0 0 0-1.81 0L3.1 4.13a2.12 2.12 0 0 0-.05 3.69l12.22 6.93a2 2 0 0 0 1.94 0L21 12.51a2.12 2.12 0 0 0-.09-3.67Z"></path>
                  <path d="m3.09 8.84 12.35-6.61a1.93 1.93 0 0 1 1.81 0l3.65 1.9a2.12 2.12 0 0 1 .1 3.69L8.73 14.75a2 2 0 0 1-1.94 0L3 12.51a2.12 2.12 0 0 1 .09-3.67Z"></path>
                  <line x1="12" x2="12" y1="22" y2="13"></line>
                  <path d="M20 13.5v3.37a2.06 2.06 0 0 1-1.11 1.83l-6 3.08a1.93 1.93 0 0 1-1.78 0l-6-3.08A2.06 2.06 0 0 1 4 16.87V13.5"></path>
                </svg>
              </div>
            </div>
          </div>
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Total Pedidos</p>
                <p className="text-3xl font-bold text-foreground mt-2">{dashboardStats?.totalOrders || 0}</p>
                <p className="text-xs text-muted-foreground mt-1">Todos los pedidos</p>
              </div>
              <div className="p-4 rounded-full bg-green-100 dark:bg-green-900">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-green-600 dark:text-green-400">
                  <circle cx="8" cy="21" r="1"></circle>
                  <circle cx="19" cy="21" r="1"></circle>
                  <path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"></path>
                </svg>
              </div>
            </div>
          </div>
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Total Usuarios</p>
                <p className="text-3xl font-bold text-foreground mt-2">{dashboardStats?.totalUsers || 0}</p>
                <p className="text-xs text-muted-foreground mt-1">Usuarios registrados</p>
              </div>
              <div className="p-4 rounded-full bg-purple-100 dark:bg-purple-900">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-purple-600 dark:text-purple-400">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
              </div>
            </div>
          </div>
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Ingresos Totales</p>
                <p className="text-3xl font-bold text-foreground mt-2">${dashboardStats?.totalRevenue?.toFixed(2) || '0.00'}</p>
                <p className="text-xs text-muted-foreground mt-1">Todas las ventas</p>
              </div>
              <div className="p-4 rounded-full bg-amber-100 dark:bg-amber-900">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-amber-600 dark:text-amber-400">
                  <line x1="12" x2="12" y1="2" y2="22"></line>
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                </svg>
              </div>
            </div>
          </div>
        </div>

        {/* Gráficos */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm">
            <h3 className="text-lg font-semibold text-foreground mb-4">Ventas por Mes</h3>
            <div className="h-72">
              <Line data={salesChartData} options={commonOptions} />
            </div>
          </div>
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm">
            <h3 className="text-lg font-semibold text-foreground mb-4">Pedidos por Estado</h3>
            <div className="h-72">
              <Doughnut data={orderStatusChartData} options={commonOptions} />
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm">
            <h3 className="text-lg font-semibold text-foreground mb-4">Productos por Categoría</h3>
            <div className="h-72">
              <Pie data={categoryChartData} options={commonOptions} />
            </div>
          </div>
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm">
            <h3 className="text-lg font-semibold text-foreground mb-4">Nuevos Usuarios por Mes</h3>
            <div className="h-72">
              <Bar data={userRegistrationsChartData} options={commonOptions} />
            </div>
          </div>
        </div>

        {/* Listas */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-card rounded-lg p-6 border border-border shadow-sm">
            <h3 className="text-lg font-semibold text-foreground mb-4">Pedidos Recientes</h3>
            <div className="overflow-hidden rounded-md border border-border">
              {dashboardStats?.recentOrders?.length ? (
                <div className="divide-y divide-border">
                  {dashboardStats?.recentOrders?.slice(0, 5).map((order) => (
                    <div key={order.id} className="flex items-center justify-between p-4 transition-colors hover:bg-muted">
                      <div>
                        <p className="font-medium text-foreground">Pedido #{order.id}</p>
                        <p className="text-sm text-muted-foreground">{order.user?.firstName} {order.user?.lastName}</p>
                      </div>
                      <div className="text-right">
                        <p className="font-medium text-foreground">${(order.total || 0).toFixed(2)}</p>
                        <div className="flex items-center gap-1 mt-1">
                          <span className={`w-2 h-2 rounded-full ${order.status === 'CONFIRMED' ? 'bg-green-500' :
                            order.status === 'PENDING' ? 'bg-amber-500' :
                              order.status === 'CANCELED' ? 'bg-red-500' : 'bg-slate-500'
                            }`}></span>
                          <p className="text-xs text-muted-foreground">{new Date(order.createdAt).toLocaleDateString()}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="p-4 text-center">
                  <p className="text-muted-foreground">No hay pedidos recientes</p>
                </div>
              )}
            </div>
          </div>

          <div className="bg-card rounded-lg p-6 border border-border shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-foreground">Productos con Stock Bajo</h3>
              <button className="text-xs text-primary hover:underline">
                Ver todos
              </button>
            </div>
            <div className="overflow-hidden rounded-md border border-border">
              {dashboardStats?.lowStockProducts?.length ? (
                <div className="divide-y divide-border">
                  {dashboardStats?.lowStockProducts?.slice(0, 5).map((product) => (
                    <div key={product.id} className="flex items-center justify-between p-4 transition-colors hover:bg-muted">
                      <div className="flex items-center gap-3">
                        <div className="h-12 w-12 rounded-md border border-border bg-muted overflow-hidden">
                          {product.imageUrl ? (
                            <img
                              src={product.imageUrl}
                              alt={product.name}
                              className="h-full w-full object-cover"
                            />
                          ) : (
                            <div className="h-full w-full flex items-center justify-center text-muted-foreground">
                              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect width="18" height="18" x="3" y="3" rx="2" ry="2"></rect>
                                <circle cx="9" cy="9" r="2"></circle>
                                <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"></path>
                              </svg>
                            </div>
                          )}
                        </div>
                        <div>
                          <p className="font-medium text-foreground">{product.name}</p>
                          <div className="flex items-center gap-1 mt-1">
                            <span className={`w-2 h-2 rounded-full ${(product.stockQuantity || 0) === 0 ? 'bg-red-500' :
                              (product.stockQuantity || 0) < 5 ? 'bg-amber-500' : 'bg-orange-300'
                              }`}></span>
                            <p className="text-xs text-muted-foreground">Stock: {product.stockQuantity}</p>
                          </div>
                        </div>
                      </div>
                      <button
                        onClick={() => handleEditProduct(product)}
                        className="px-3 py-1.5 text-xs bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
                      >
                        Editar
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="p-4 text-center">
                  <p className="text-muted-foreground">No hay productos con stock bajo</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  };

  const [productSearchQuery, setProductSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const handleProductSearch = async () => {
    if (!productSearchQuery.trim()) {
      await fetchAdminProducts();
      return;
    }

    setIsSearching(true);
    try {
      const response = await adminProductAPI.searchProducts(productSearchQuery.trim());
      setAdminProducts(response.data.content || []);
    } catch (error) {
      // Error handling removed for production mode
    } finally {
      setIsSearching(false);
    }
  };

  const renderProducts = () => (
    <div className="space-y-6">
      {adminProductsLoading || isSearching ? (
        <div className="flex justify-center items-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          <span className="ml-2 text-muted-foreground">Cargando productos...</span>
        </div>
      ) : (
        <>
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold text-foreground">Gestión de Productos</h2>
            <button
              onClick={handleAddProduct}
              className="bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2 rounded-md transition-colors"
            >
              Agregar Producto
            </button>
          </div>

          {/* Buscador de productos */}
          <div className="w-full flex my-4 gap-2">
            <input
              type="text"
              value={productSearchQuery}
              onChange={(e) => setProductSearchQuery(e.target.value)}
              placeholder="Buscar productos por nombre o descripción..."
              className="flex-1 px-4 py-2 border border-border rounded-md bg-background text-foreground"
              onKeyDown={(e) => e.key === 'Enter' && handleProductSearch()}
            />
            <button
              onClick={handleProductSearch}
              className="bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2 rounded-md transition-colors"
            >
              Buscar
            </button>
            {productSearchQuery && (
              <button
                onClick={() => {
                  setProductSearchQuery('');
                  fetchAdminProducts();
                }}
                className="bg-destructive hover:bg-destructive/90 text-destructive-foreground px-4 py-2 rounded-md transition-colors"
              >
                Limpiar
              </button>
            )}
          </div>

          <div className="bg-card rounded-lg border border-border overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full border-collapse bg-card/50">
                <thead className="bg-muted">
                  <tr>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Producto</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Precio</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Stock</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Estado</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {adminProducts.map((product) => (
                    <tr key={product.id} className="hover:bg-muted/50">
                      <td className="px-4 py-3">
                        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => navigate(`/product/${product.id}`)}>
                          <img
                            src={product.imageUrl || '/placeholder.png'}
                            alt={product.name}
                            className="w-10 h-10 rounded-md object-cover"
                          />
                          <div>
                            <p className="font-medium text-foreground text-blue-600 hover:text-blue-800 hover:underline">{product.name}</p>
                            <p className="text-sm text-muted-foreground">{product.sku}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-foreground">${(product.price || 0).toFixed(2)}</td>
                      <td className="px-4 py-3 text-foreground">{product.stockQuantity}</td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${product.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                          {product.active ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => handleEditProduct(product)}
                            className="text-primary hover:text-primary/80 text-sm font-medium"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => handleSoftDeleteProduct(product.id)}
                            className="text-orange-600 hover:text-orange-800 text-sm font-medium"
                          >
                            Desactivar
                          </button>
                          <button
                            onClick={() => handleHardDeleteProduct(product.id, product.name)}
                            className="text-destructive hover:text-destructive/80 text-sm font-medium"
                          >
                            Eliminar
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-muted">
                  <tr>
                    <td colSpan={5} className="px-4 py-3 text-sm">
                      {productSearchQuery ? `${adminProducts.length} productos encontrados` : `${adminProducts.length} productos en total`}
                      {adminProducts.some(p => !p.active) && ` (incluye productos inactivos)`}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          {/* Paginación de productos */}
          {adminProducts && adminProducts.length > 0 && (
            <Pagination
              currentPage={productPage}
              totalPages={productTotalPages}
              totalElements={productTotalElements}
              size={productSize}
              onPageChange={handleProductPageChange}
            />
          )}
        </>
      )}
    </div>
  );

  const renderOrders = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold text-foreground">Gestión de Pedidos</h2>
        <button
          onClick={handleCheckPendingPayments}
          disabled={checkingPendingPayments}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium"
        >
          {checkingPendingPayments ? 'Verificando...' : 'Verificar Pagos Pendientes'}
        </button>
      </div>

      <div className="bg-card rounded-lg border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">ID</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Cliente</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Total</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Estado</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Fecha</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {orders.map((order) => (
                <tr key={order.id} className="hover:bg-muted/50">
                  <td className="px-4 py-3 font-medium text-foreground">#{order.id}</td>
                  <td className="px-4 py-3 text-foreground">
                    {order.user?.firstName} {order.user?.lastName}
                  </td>
                  <td className="px-4 py-3 text-foreground">${(order.total || 0).toFixed(2)}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${order.status === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                      order.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                        'bg-red-100 text-red-800'
                      }`}>
                      {order.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {new Date(order.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleViewOrderDetails(order)}
                      className="text-primary hover:text-primary/80 text-sm font-medium">
                      Ver Detalles
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );

  const renderUsers = () => (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-foreground">Gestión de Usuarios</h2>

      <div className="bg-card rounded-lg border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Usuario</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Email</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Rol</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Estado</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-muted/50">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-foreground">{user.firstName} {user.lastName}</p>
                      <p className="text-sm text-muted-foreground">ID: {user.id}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-foreground">{user.email}</td>
                  <td className="px-4 py-3 text-foreground capitalize">{user.role}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${user.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                      {user.active ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => handleChangeUserRole(user.id, `${user.firstName} ${user.lastName}`, user.role)}
                        className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                      >
                        Cambiar Rol
                      </button>
                      <button
                        onClick={() => handleSoftDeleteUser(user.id)}
                        className="text-orange-600 hover:text-orange-800 text-sm font-medium"
                      >
                        Desactivar
                      </button>
                      <button
                        onClick={() => handleHardDeleteUser(user.id, `${user.firstName} ${user.lastName}`)}
                        className="text-destructive hover:text-destructive/80 text-sm font-medium"
                      >
                        Eliminar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Paginación de usuarios */}
        {users && users.length > 0 && (
          <Pagination
            currentPage={userPage}
            totalPages={userTotalPages}
            totalElements={userTotalElements}
            size={userSize}
            onPageChange={handleUserPageChange}
          />
        )}
      </div>
    </div>
  );

  const renderReviews = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-foreground">Gestión de Reseñas</h2>
        <button
          onClick={() => {
            setReviewPage(0);
            fetchReviews(0);
          }}
          className="px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-md transition-colors"
        >
          Cargar Reseñas
        </button>
      </div>

      {reviewsLoading ? (
        <div className="flex justify-center items-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          <span className="ml-2 text-muted-foreground">Cargando reseñas...</span>
        </div>
      ) : reviews.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No hay reseñas para mostrar
        </div>
      ) : (
        <>
          <div className="overflow-x-auto border border-border rounded-lg">
            <table className="w-full">
              <thead className="bg-muted">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-foreground">Producto</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-foreground">Usuario</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-foreground">Rating</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-foreground">Comentario</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-foreground">Estado</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-foreground">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {reviews.map((review: any) => (
                  <tr key={review.id} className="hover:bg-muted/50 transition-colors">
                    <td className="px-4 py-3 text-sm">{review.productName}</td>
                    <td className="px-4 py-3 text-sm">{review.user?.firstName || review.user?.email}</td>
                    <td className="px-4 py-3 text-sm">
                      <div className="flex items-center">
                        {[...Array(5)].map((_, i) => (
                          <span key={i} className={i < review.rating ? 'text-yellow-400' : 'text-gray-300'}>
                            ★
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm truncate max-w-xs">{review.comment || '-'}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${review.status === 'APPROVED'
                        ? 'bg-green-100 text-green-800'
                        : review.status === 'REJECTED'
                          ? 'bg-red-100 text-red-800'
                          : 'bg-yellow-100 text-yellow-800'
                        }`}>
                        {review.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm space-x-2">
                      <button
                        onClick={() => handleDeleteReview(review.id, review.productName)}
                        className="px-3 py-1 bg-red-500 hover:bg-red-600 text-white rounded text-xs transition-colors"
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {reviews && reviews.length > 0 && (
            <Pagination
              currentPage={reviewPage}
              totalPages={reviewTotalPages}
              totalElements={reviewTotalElements}
              size={reviewSize}
              onPageChange={handleReviewPageChange}
            />
          )}
        </>
      )}
    </div>
  );

  // Variant Attributes functions
  const fetchVariantAttributes = async () => {
    setVariantAttributesLoading(true);
    try {
      const response = await adminVariantAttributeAPI.getAttributes();
      setVariantAttributes(response.data);
    } catch (error) {
      // Error handling removed for production mode
    } finally {
      setVariantAttributesLoading(false);
    }
  };

  const handleCreateVariantAttribute = async () => {
    // Validación: si no es global, debe tener categorías seleccionadas
    if (!newVariantAttribute.global && selectedCategories.length === 0) {
      alert('Debe seleccionar al menos una categoría para atributos específicos por categoría.');
      return;
    }

    try {
      const attributeToCreate = {
        name: newVariantAttribute.name,
        displayName: newVariantAttribute.displayName,
        description: newVariantAttribute.description,
        type: newVariantAttribute.attributeType, // Map attributeType to type
        unit: newVariantAttribute.unit,
        options: newVariantAttribute.options,
        required: newVariantAttribute.required,
        global: newVariantAttribute.global,
        categoryIds: newVariantAttribute.global ? [] : selectedCategories
      };
      await adminVariantAttributeAPI.createAttribute(attributeToCreate);
      setShowVariantAttributeModal(false);
      setNewVariantAttribute({
        name: '',
        displayName: '',
        description: '',
        attributeType: 'TEXT',
        unit: '',
        options: [],
        required: false,
        global: true,
        categoryIds: []
      });
      setSelectedCategories([]);
      fetchVariantAttributes();
    } catch (error) {
      // Error handling removed for production mode
    }
  };

  const handleUpdateVariantAttribute = async () => {
    if (!editingVariantAttribute) return;

    // Validación: si no es global, debe tener categorías seleccionadas
    if (!editingVariantAttribute.global && selectedCategories.length === 0) {
      alert('Debe seleccionar al menos una categoría para atributos específicos por categoría.');
      return;
    }

    try {
      const attributeToUpdate = {
        name: editingVariantAttribute.name,
        displayName: editingVariantAttribute.displayName,
        description: editingVariantAttribute.description,
        type: editingVariantAttribute.attributeType, // Map attributeType to type
        unit: editingVariantAttribute.unit,
        options: editingVariantAttribute.options,
        required: editingVariantAttribute.required,
        global: editingVariantAttribute.global,
        categoryIds: editingVariantAttribute.global ? [] : selectedCategories
      };
      await adminVariantAttributeAPI.updateAttribute(editingVariantAttribute.id, attributeToUpdate);
      setShowVariantAttributeModal(false);
      setEditingVariantAttribute(null);
      setSelectedCategories([]);
      fetchVariantAttributes();
    } catch (error) {
      // Error handling removed for production mode
    }
  };

  const handleDeleteVariantAttribute = async (id: number) => {
    if (!confirm('¿Estás seguro de que quieres eliminar este atributo de variante?')) return;
    try {
      await adminVariantAttributeAPI.deleteAttribute(id);
      fetchVariantAttributes();
    } catch (error) {
      // Error handling removed for production mode
    }
  };

  const renderCategories = () => (
    <div className="space-y-6">
      {categoriesLoading ? (
        <div className="flex justify-center items-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          <span className="ml-2 text-muted-foreground">Cargando categorías...</span>
        </div>
      ) : (
        <>
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold text-foreground">Gestión de Categorías</h2>
            <button
              onClick={openCreateCategoryModal}
              className="bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2 rounded-md transition-colors"
            >
              Agregar Categoría
            </button>
          </div>

          <div className="bg-card rounded-lg border border-border overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full border-collapse bg-card/50">
                <thead className="bg-muted">
                  <tr>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Nombre</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Descripción</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Orden</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Estado</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {categories.map((category) => (
                    <tr key={category.id} className="hover:bg-muted/50">
                      <td className="px-4 py-3">
                        <div>
                          <p className="font-medium text-foreground">{category.name}</p>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-foreground">{category.description || '-'}</td>
                      <td className="px-4 py-3 text-foreground">{category.sortOrder || 0}</td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${(category as any).active !== false ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                          {(category as any).active !== false ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => openEditCategoryModal(category)}
                            className="text-primary hover:text-primary/80 text-sm font-medium"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => handleDeleteCategory(Number(category.id))}
                            className="text-red-600 hover:text-red-800 text-sm font-medium"
                          >
                            Eliminar
                          </button>
                          {(category as any).active === false && (
                            <button
                              onClick={() => handleRestoreCategory(Number(category.id))}
                              className="text-green-600 hover:text-green-800 text-sm font-medium"
                            >
                              Restaurar
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );

  const renderVariants = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-foreground">Atributos de Variantes</h2>
        <button
          onClick={() => {
            setEditingVariantAttribute(null);
            setNewVariantAttribute({
              name: '',
              displayName: '',
              description: '',
              attributeType: 'TEXT',
              unit: '',
              options: [],
              required: false,
              global: true,
              categoryIds: []
            });
            setShowVariantAttributeModal(true);
          }}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
        >
          Agregar Atributo
        </button>
      </div>

      {variantAttributesLoading ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="mt-2 text-muted-foreground">Cargando atributos...</p>
        </div>
      ) : (
        <div className="bg-card rounded-lg border border-border overflow-hidden">
          <table className="w-full">
            <thead className="bg-muted">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Nombre</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Tipo</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Alcance</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Requerido</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Estado</th>
                <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {variantAttributes.map((attribute) => (
                <tr key={attribute.id} className="hover:bg-muted/50">
                  <td className="px-4 py-3">
                    <div>
                      <div className="font-medium text-foreground">{attribute.displayName}</div>
                      <div className="text-sm text-muted-foreground">{attribute.name}</div>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300">
                      {attribute.attributeType}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    {attribute.global ? (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300">
                        Global
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300">
                        Específico
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {attribute.required ? (
                      <span className="text-green-600">✓</span>
                    ) : (
                      <span className="text-muted-foreground">✗</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {attribute.active ? (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300">
                        Activo
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300">
                        Inactivo
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => {
                        setEditingVariantAttribute(attribute);
                        // Inicializar selectedCategories con las categorías del atributo si no es global
                        // Nota: Por ahora asumimos que el backend no devuelve categoryIds en la respuesta
                        // En el futuro, si el backend los incluye, usar: setSelectedCategories(attribute.categoryIds || []);
                        setSelectedCategories([]);
                        setShowVariantAttributeModal(true);
                      }}
                      className="text-blue-600 hover:text-blue-800 mr-2"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => handleDeleteVariantAttribute(attribute.id)}
                      className="text-red-600 hover:text-red-800"
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );

  const renderAudit = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold text-foreground">Registro de Auditoría</h2>
        <div className="flex space-x-2">
          <button
            onClick={async () => {
              try {
                await auditAPI.createTestData();
                alert('Datos de prueba creados exitosamente');
                fetchAuditLogs();
              } catch (error) {
                // Error handling removed for production mode
                alert('Error al crear datos de prueba');
              }
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm font-medium"
          >
            Crear Datos de Prueba
          </button>
          <button
            onClick={async () => {
              try {
                const response = await auditAPI.getAuditCount();
                alert(`Total de logs de auditoría: ${response.data.total}`);
              } catch (error) {
                // Error handling removed for production mode
                alert('Error al obtener conteo');
              }
            }}
            className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm font-medium"
          >
            Ver Total
          </button>
          <button
            onClick={clearAuditFilters}
            className="px-4 py-2 bg-secondary text-secondary-foreground rounded-md hover:bg-secondary/80 text-sm font-medium"
          >
            Limpiar Filtros
          </button>
        </div>
      </div>

      {/* Filtros */}
      <div className="bg-card rounded-lg border border-border p-4">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Usuario</label>
            <input
              type="text"
              value={auditFilters.userEmail || ''}
              onChange={(e) => handleAuditFilterChange('userEmail', e.target.value || undefined)}
              placeholder="Buscar por email..."
              className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Acción</label>
            <select
              value={auditFilters.action || ''}
              onChange={(e) => handleAuditFilterChange('action', e.target.value || undefined)}
              className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
            >
              <option value="">Todas las acciones</option>
              <option value="LOGIN">Inicio de sesión</option>
              <option value="USER_CREATE">Crear usuario</option>
              <option value="PRODUCT_CREATE">Crear producto</option>
              <option value="ORDER_CREATE">Crear pedido</option>
              <option value="USER_UPDATE">Actualizar usuario</option>
              <option value="PRODUCT_UPDATE">Actualizar producto</option>
              <option value="ORDER_UPDATE">Actualizar pedido</option>
              <option value="USER_DELETE">Eliminar usuario</option>
              <option value="PRODUCT_DELETE">Eliminar producto</option>
              <option value="ORDER_CANCEL">Cancelar pedido</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Tipo de Entidad</label>
            <select
              value={auditFilters.entityType || ''}
              onChange={(e) => handleAuditFilterChange('entityType', e.target.value || undefined)}
              className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
            >
              <option value="">Todos los tipos</option>
              <option value="User">Usuario</option>
              <option value="Product">Producto</option>
              <option value="Order">Pedido</option>
              <option value="Category">Categoría</option>
              <option value="Payment">Pago</option>
              <option value="Review">Reseña</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Fecha Desde</label>
            <input
              type="date"
              value={auditFilters.startDate ? auditFilters.startDate.split('T')[0] : ''}
              onChange={(e) => handleAuditFilterChange('startDate', e.target.value ? e.target.value : undefined)}
              className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Fecha Hasta</label>
            <input
              type="date"
              value={auditFilters.endDate ? auditFilters.endDate.split('T')[0] : ''}
              onChange={(e) => handleAuditFilterChange('endDate', e.target.value ? e.target.value : undefined)}
              className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">Estado</label>
            <select
              value={auditFilters.success === undefined ? '' : auditFilters.success.toString()}
              onChange={(e) => handleAuditFilterChange('success', e.target.value === '' ? undefined : e.target.value === 'true')}
              className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
            >
              <option value="">Todos</option>
              <option value="true">Exitoso</option>
              <option value="false">Fallido</option>
            </select>
          </div>
        </div>
      </div>

      {/* Tabla de auditoría */}
      <div className="bg-card rounded-lg border border-border overflow-hidden">
        {auditLoading ? (
          <div className="p-8 text-center text-muted-foreground">Cargando registros de auditoría...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted">
                <tr>
                  <th
                    className="px-4 py-3 text-left text-sm font-medium text-muted-foreground cursor-pointer hover:bg-muted/80"
                    onClick={() => handleAuditSort('action')}
                  >
                    Acción {auditSortBy === 'action' && (auditSortDir === 'asc' ? '↑' : '↓')}
                  </th>
                  <th
                    className="px-4 py-3 text-left text-sm font-medium text-muted-foreground cursor-pointer hover:bg-muted/80"
                    onClick={() => handleAuditSort('entityType')}
                  >
                    Entidad {auditSortBy === 'entityType' && (auditSortDir === 'asc' ? '↑' : '↓')}
                  </th>
                  <th
                    className="px-4 py-3 text-left text-sm font-medium text-muted-foreground cursor-pointer hover:bg-muted/80"
                    onClick={() => handleAuditSort('userEmail')}
                  >
                    Usuario {auditSortBy === 'userEmail' && (auditSortDir === 'asc' ? '↑' : '↓')}
                  </th>
                  <th
                    className="px-4 py-3 text-left text-sm font-medium text-muted-foreground cursor-pointer hover:bg-muted/80"
                    onClick={() => handleAuditSort('timestamp')}
                  >
                    Fecha {auditSortBy === 'timestamp' && (auditSortDir === 'asc' ? '↑' : '↓')}
                  </th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Estado</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Detalles</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {auditLogs.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                      {auditLoading ? 'Cargando registros de auditoría...' : 'No se encontraron registros de auditoría'}
                    </td>
                  </tr>
                ) : (
                  auditLogs.map((log) => (
                    <tr key={log.id} className="hover:bg-muted/50">
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${log.action.includes('CREATE') ? 'bg-green-100 text-green-800' :
                          log.action.includes('UPDATE') ? 'bg-blue-100 text-blue-800' :
                            log.action.includes('DELETE') ? 'bg-red-100 text-red-800' :
                              log.action.includes('LOGIN') ? 'bg-purple-100 text-purple-800' :
                                'bg-gray-100 text-gray-800'
                          }`}>
                          {formatAuditAction(log.action)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-foreground">
                        <div>
                          <p className="font-medium">{formatEntityType(log.entityType)}</p>
                          <p className="text-sm text-muted-foreground">ID: {log.entityId}</p>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-foreground">
                        {log.userEmail || 'Sistema'}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        <div>
                          <p>{new Date(log.timestamp).toLocaleDateString()}</p>
                          <p className="text-sm">{new Date(log.timestamp).toLocaleTimeString()}</p>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${log.success ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                          {log.success ? 'Exitoso' : 'Fallido'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground max-w-xs">
                        <div className="truncate" title={log.details || log.errorMessage || 'Sin detalles'}>
                          {log.details || log.errorMessage || 'Sin detalles'}
                        </div>
                        {log.executionTimeMs && (
                          <p className="text-xs text-muted-foreground mt-1">
                            {log.executionTimeMs}ms
                          </p>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Paginación */}
        {auditTotalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 bg-muted/50 border-t border-border">
            <div className="text-sm text-muted-foreground">
              Página {auditPage + 1} de {auditTotalPages}
            </div>
            <div className="flex space-x-2">
              <button
                onClick={() => setAuditPage(Math.max(0, auditPage - 1))}
                disabled={auditPage === 0}
                className="px-3 py-1 text-sm border border-border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-muted"
              >
                Anterior
              </button>
              <button
                onClick={() => setAuditPage(Math.min(auditTotalPages - 1, auditPage + 1))}
                disabled={auditPage === auditTotalPages - 1}
                className="px-3 py-1 text-sm border border-border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-muted"
              >
                Siguiente
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      // console.log('Admin user detected, loading initial data...');
      fetchDashboardStats();
      fetchAllOrders();
      fetchAllUsers(0, userSize);
      fetchAuditLogs();
    }
  }, [user]);

  useEffect(() => {
    if (user?.role === 'ADMIN' && activeTab === 'audit') {
      // console.log('Audit tab activated, fetching audit logs...');
      // console.log('Current audit state:', { auditPage, auditSortBy, auditSortDir, auditFilters });
      fetchAuditLogs();
    }
  }, [auditPage, auditSortBy, auditSortDir, auditFilters, activeTab, user]);

  useEffect(() => {
    if (user?.role === 'ADMIN' && activeTab === 'products' && adminProducts.length === 0) {
      fetchAdminProducts(0, productSize);
    }
  }, [user, activeTab, adminProducts.length]);

  useEffect(() => {
    if (user?.role === 'ADMIN' && activeTab === 'variants' && variantAttributes.length === 0) {
      fetchVariantAttributes();
    }
  }, [user, activeTab, variantAttributes.length]);

  useEffect(() => {
    if (user?.role === 'ADMIN' && activeTab === 'categories' && categories.length === 0) {
      fetchCategories();
    }
  }, [user, activeTab, categories.length]);

  useEffect(() => {
    if (user?.role === 'ADMIN' && activeTab === 'reviews') {
      fetchReviews(0, reviewSize);
    }
  }, [user, activeTab]);

  if (!user || user.role !== 'ADMIN') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-foreground mb-2">Acceso Denegado</h1>
          <p className="text-muted-foreground">No tienes permisos para acceder a esta página.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8">
        <div className="mb-8 bg-card p-6 rounded-lg shadow-sm border border-border">
          <div className="flex justify-between items-center mb-4">
            <div>
              <h1 className="text-3xl font-bold text-foreground mb-2">Panel de Administración</h1>
              <p className="text-muted-foreground">Gestiona productos, pedidos y usuarios</p>
            </div>
            <button
              onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
              className="p-2 bg-accent hover:bg-accent/80 rounded-full"
              aria-label="Cambiar tema"
              title={`Cambiar a modo ${theme === 'dark' ? 'claro' : 'oscuro'}`}
            >
              {theme === 'dark' ? (
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 3v2.25m6.364.386-1.591 1.591M21 12h-2.25m-.386 6.364-1.591-1.591M12 18.75V21m-4.773-4.227-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z" />
                </svg>
              ) : (
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21.752 15.002A9.72 9.72 0 0 1 18 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 0 0 3 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 0 0 9.002-5.998Z" />
                </svg>
              )}
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex overflow-x-auto gap-1 mb-8 bg-card border border-border p-2 rounded-lg shadow-sm">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center justify-center gap-2 px-5 py-3 rounded-md text-sm font-medium transition-colors whitespace-nowrap ${activeTab === tab.id
                ? 'bg-primary text-primary-foreground shadow-md'
                : 'bg-card text-foreground hover:bg-accent hover:text-accent-foreground'
                }`}
            >
              <span className="text-lg">{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="bg-card rounded-lg shadow-sm border border-border p-6 space-y-6">
          {activeTab === 'dashboard' && renderDashboard()}
          {activeTab === 'products' && renderProducts()}
          {activeTab === 'categories' && renderCategories()}
          {activeTab === 'variants' && renderVariants()}
          {activeTab === 'orders' && renderOrders()}
          {activeTab === 'reviews' && renderReviews()}
          {activeTab === 'users' && renderUsers()}
          {activeTab === 'audit' && renderAudit()}
        </div>

        {/* Edit Product Modal */}
        {showEditProductModal && (editingProduct || newProduct) && (
          <div className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-start justify-center z-50 p-4 overflow-y-auto">
            <div className="bg-card rounded-lg shadow-xl max-w-2xl w-full mt-8 mb-8 border border-border">
              <div className="p-6">
                <h3 className="text-lg font-medium text-foreground mb-4">
                  {editingProductId ? 'Editar Producto' : 'Agregar Nuevo Producto'}
                </h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Nombre *
                    </label>
                    <input
                      type="text"
                      value={editingProduct ? editingProduct.name : newProduct.name}
                      onChange={(e) => {
                        if (editingProduct) {
                          setEditingProduct({ ...editingProduct, name: e.target.value });
                        } else {
                          setNewProduct({ ...newProduct, name: e.target.value });
                        }
                      }}
                      className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Descripción
                    </label>
                    <textarea
                      value={editingProduct ? (editingProduct.description || '') : newProduct.description}
                      onChange={(e) => {
                        if (editingProduct) {
                          setEditingProduct({ ...editingProduct, description: e.target.value });
                        } else {
                          setNewProduct({ ...newProduct, description: e.target.value });
                        }
                      }}
                      rows={3}
                      className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Descripción Corta
                    </label>
                    <textarea
                      value={editingProduct ? ((editingProduct as any).shortDescription || '') : newProduct.shortDescription}
                      onChange={(e) => {
                        if (editingProduct) {
                          setEditingProduct({ ...editingProduct, shortDescription: e.target.value } as any);
                        } else {
                          setNewProduct({ ...newProduct, shortDescription: e.target.value });
                        }
                      }}
                      rows={2}
                      className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                      placeholder="Descripción breve del producto..."
                    />
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        SKU
                      </label>
                      <input
                        type="text"
                        value={editingProduct ? (editingProduct.sku || '') : newProduct.sku}
                        onChange={(e) => {
                          if (editingProduct) {
                            setEditingProduct({ ...editingProduct, sku: e.target.value });
                          } else {
                            setNewProduct({ ...newProduct, sku: e.target.value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="Código único del producto..."
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Código de Barras
                      </label>
                      <input
                        type="text"
                        value={editingProduct ? ((editingProduct as any).barcode || '') : newProduct.barcode}
                        onChange={(e) => {
                          if (editingProduct) {
                            setEditingProduct({ ...editingProduct, barcode: e.target.value } as any);
                          } else {
                            setNewProduct({ ...newProduct, barcode: e.target.value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="Código EAN/UPC..."
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Precio *
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        min="0.01"
                        value={editingProduct ? editingProduct.price : newProduct.price}
                        onChange={(e) => {
                          const value = parseFloat(e.target.value) || 0;
                          if (editingProduct) {
                            setEditingProduct({ ...editingProduct, price: value });
                          } else {
                            setNewProduct({ ...newProduct, price: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Precio Comparativo
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        value={editingProduct ? ((editingProduct as any).compareAtPrice || 0) : newProduct.compareAtPrice}
                        onChange={(e) => {
                          const value = parseFloat(e.target.value) || 0;
                          if (editingProduct) {
                            setEditingProduct({ ...editingProduct, compareAtPrice: value } as any);
                          } else {
                            setNewProduct({ ...newProduct, compareAtPrice: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="Precio anterior (tachado)..."
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Imagen del Producto
                    </label>
                    <div className="space-y-3">
                      <div className="flex items-center space-x-3">
                        <input
                          type="file"
                          accept="image/*"
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) handleImageUpload(file, !!editingProduct);
                          }}
                          disabled={uploadingImage}
                          className="block w-full text-sm text-muted-foreground file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 disabled:opacity-50"
                        />
                        {uploadingImage && (
                          <div className="flex items-center text-primary">
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary mr-2"></div>
                            Subiendo...
                          </div>
                        )}
                      </div>
                      <input
                        type="url"
                        value={editingProduct ? (editingProduct.imageUrl || '') : newProduct.imageUrl}
                        onChange={(e) => {
                          if (editingProduct) {
                            setEditingProduct({ ...editingProduct, imageUrl: e.target.value });
                          } else {
                            setNewProduct({ ...newProduct, imageUrl: e.target.value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="https://ejemplo.com/imagen.jpg o sube una imagen arriba"
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Cantidad en Stock *
                      </label>
                      <input
                        type="number"
                        min="0"
                        value={editingProduct ? editingProduct.stockQuantity : newProduct.stockQuantity}
                        onChange={(e) => {
                          const value = parseInt(e.target.value) || 0;
                          if (editingProduct) {
                            setEditingProduct({ ...editingProduct, stockQuantity: value });
                          } else {
                            setNewProduct({ ...newProduct, stockQuantity: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Límite de Stock Bajo
                      </label>
                      <input
                        type="number"
                        min="0"
                        value={editingProduct ? ((editingProduct as any).lowStockThreshold || 5) : newProduct.lowStockThreshold}
                        onChange={(e) => {
                          const value = parseInt(e.target.value) || 5;
                          if (editingProduct) {
                            setEditingProduct({ ...editingProduct, lowStockThreshold: value } as any);
                          } else {
                            setNewProduct({ ...newProduct, lowStockThreshold: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="Avisar cuando el stock sea menor a..."
                      />
                    </div>
                  </div>

                  {/* Sección SEO */}
                  <div className="border-t border-border pt-4">
                    <h4 className="text-md font-medium text-foreground mb-3">Configuración SEO</h4>
                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          Título SEO
                        </label>
                        <input
                          type="text"
                          value={editingProduct ? ((editingProduct as any).seoTitle || '') : newProduct.seoTitle}
                          onChange={(e) => {
                            if (editingProduct) {
                              setEditingProduct({ ...editingProduct, seoTitle: e.target.value } as any);
                            } else {
                              setNewProduct({ ...newProduct, seoTitle: e.target.value });
                            }
                          }}
                          className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                          placeholder="Título optimizado para motores de búsqueda..."
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          Descripción SEO
                        </label>
                        <textarea
                          value={editingProduct ? ((editingProduct as any).seoDescription || '') : newProduct.seoDescription}
                          onChange={(e) => {
                            if (editingProduct) {
                              setEditingProduct({ ...editingProduct, seoDescription: e.target.value } as any);
                            } else {
                              setNewProduct({ ...newProduct, seoDescription: e.target.value });
                            }
                          }}
                          rows={2}
                          className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                          placeholder="Meta descripción para motores de búsqueda..."
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-2">
                          Palabras Clave SEO
                        </label>
                        <input
                          type="text"
                          value={editingProduct ? ((editingProduct as any).seoKeywords || '') : newProduct.seoKeywords}
                          onChange={(e) => {
                            if (editingProduct) {
                              setEditingProduct({ ...editingProduct, seoKeywords: e.target.value } as any);
                            } else {
                              setNewProduct({ ...newProduct, seoKeywords: e.target.value });
                            }
                          }}
                          className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                          placeholder="Palabras clave separadas por comas..."
                        />
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      id="productActive"
                      checked={editingProduct ? ((editingProduct as any).active !== false) : newProduct.active}
                      onChange={(e) => {
                        if (editingProduct) {
                          setEditingProduct({ ...editingProduct, active: e.target.checked });
                        } else {
                          setNewProduct({ ...newProduct, active: e.target.checked });
                        }
                      }}
                      className="rounded border-border text-primary focus:ring-primary"
                    />
                    <label htmlFor="productActive" className="text-sm font-medium text-foreground">
                      Producto activo
                    </label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      id="productFeatured"
                      checked={editingProduct ? ((editingProduct as any).featured !== false) : newProduct.featured}
                      onChange={(e) => {
                        if (editingProduct) {
                          setEditingProduct({ ...editingProduct, featured: e.target.checked });
                        } else {
                          setNewProduct({ ...newProduct, featured: e.target.checked });
                        }
                      }}
                      className="rounded border-border text-primary focus:ring-primary"
                    />
                    <label htmlFor="productFeatured" className="text-sm font-medium text-foreground">
                      Producto destacado
                    </label>
                  </div>
                  <div className="flex justify-end space-x-3 mt-6">
                    <button
                      onClick={() => {
                        setShowEditProductModal(false);
                        setEditingProduct(null);
                        setEditingProductId(null);
                        setNewProduct({
                          name: '',
                          description: '',
                          shortDescription: '',
                          price: 0,
                          compareAtPrice: 0,
                          stockQuantity: 0,
                          lowStockThreshold: 5,
                          sku: '',
                          barcode: '',
                          categoryId: null,
                          imageUrl: '',
                          active: true,
                          featured: false,
                          seoTitle: '',
                          seoDescription: '',
                          seoKeywords: ''
                        });
                      }}
                      className="px-4 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors"
                    >
                      Cancelar
                    </button>
                    <button
                      onClick={editingProductId ? handleUpdateProduct : handleCreateProduct}
                      className="px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-md transition-colors"
                    >
                      {editingProductId ? 'Actualizar Producto' : 'Crear Producto'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Category Modal */}
        {showCategoryModal && (
          <div className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-start justify-center z-50 p-4 overflow-y-auto">
            <div className="bg-card rounded-lg shadow-xl max-w-2xl w-full mt-8 mb-8 border border-border">
              <div className="p-6">
                <h3 className="text-lg font-medium text-foreground mb-4">
                  {editingCategory ? 'Editar Categoría' : 'Agregar Nueva Categoría'}
                </h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Nombre *
                    </label>
                    <input
                      type="text"
                      value={editingCategory ? editingCategory.name : newCategory.name}
                      onChange={(e) => {
                        if (editingCategory) {
                          setEditingCategory({ ...editingCategory, name: e.target.value });
                        } else {
                          setNewCategory({ ...newCategory, name: e.target.value });
                        }
                      }}
                      className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Descripción
                    </label>
                    <textarea
                      value={editingCategory ? (editingCategory.description || '') : newCategory.description}
                      onChange={(e) => {
                        if (editingCategory) {
                          setEditingCategory({ ...editingCategory, description: e.target.value });
                        } else {
                          setNewCategory({ ...newCategory, description: e.target.value });
                        }
                      }}
                      rows={3}
                      className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Orden de Visualización
                    </label>
                    <input
                      type="number"
                      min="0"
                      value={editingCategory ? (editingCategory.sortOrder || 0) : newCategory.sortOrder}
                      onChange={(e) => {
                        const value = parseInt(e.target.value) || 0;
                        if (editingCategory) {
                          setEditingCategory({ ...editingCategory, sortOrder: value });
                        } else {
                          setNewCategory({ ...newCategory, sortOrder: value });
                        }
                      }}
                      className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                    />
                  </div>
                  <div className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      id="categoryActive"
                      checked={editingCategory ? ((editingCategory as any).active !== false) : newCategory.active}
                      onChange={(e) => {
                        if (editingCategory) {
                          setEditingCategory({ ...editingCategory, active: e.target.checked });
                        } else {
                          setNewCategory({ ...newCategory, active: e.target.checked });
                        }
                      }}
                      className="rounded border-border text-primary focus:ring-primary"
                    />
                    <label htmlFor="categoryActive" className="text-sm font-medium text-foreground">
                      Categoría activa
                    </label>
                  </div>
                  <div className="flex justify-end space-x-3 mt-6">
                    <button
                      onClick={() => {
                        setShowCategoryModal(false);
                        setEditingCategory(null);
                        setNewCategory({ name: '', description: '', parentId: null, sortOrder: 0, active: true });
                      }}
                      className="px-4 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors"
                    >
                      Cancelar
                    </button>
                    <button
                      onClick={editingCategory ? handleUpdateCategory : handleCreateCategory}
                      className="px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-md transition-colors"
                    >
                      {editingCategory ? 'Actualizar Categoría' : 'Crear Categoría'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Hard Delete Confirmation Modal */}
        {showHardDeleteModal && hardDeleteTarget && (
          <div className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-card rounded-lg shadow-lg max-w-lg w-full z-10 border border-border">
              <div className="bg-destructive px-6 py-4 rounded-t-lg">
                <h3 className="text-xl font-semibold text-destructive-foreground flex items-center space-x-2">
                  <span>️</span>
                  <span>Eliminar Permanentemente</span>
                </h3>
              </div>
              <div className="p-6">
                <p className="text-sm text-muted-foreground mb-4">
                  Estás a punto de eliminar permanentemente {hardDeleteTarget.type === 'product' ? 'el producto' : 'al usuario'} "
                  <span className="font-semibold text-foreground">{hardDeleteTarget.name}</span>". Esta acción no se puede deshacer.
                </p>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Confirma tu contraseña de administrador
                  </label>
                  <input
                    type="password"
                    value={hardDeletePassword}
                    onChange={(e) => setHardDeletePassword(e.target.value)}
                    className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                    placeholder="Ingresa tu contraseña"
                    required
                  />
                </div>
                <div className="flex justify-end space-x-3">
                  <button
                    onClick={() => {
                      setShowHardDeleteModal(false);
                      setHardDeleteTarget(null);
                      setHardDeletePassword('');
                    }}
                    className="px-6 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors font-medium"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={confirmHardDelete}
                    disabled={!hardDeletePassword || hardDeleteLoading}
                    className="px-6 py-2 bg-destructive hover:bg-destructive/90 disabled:bg-destructive/50 text-destructive-foreground rounded-md transition-colors flex items-center"
                  >
                    {hardDeleteLoading ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-destructive-foreground mr-2"></div>
                        Eliminando...
                      </>
                    ) : (
                      'Eliminar Permanentemente'
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Role Change Modal */}
        {showRoleChangeModal && roleChangeTarget && (
          <div className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center z-50">
            <div className="bg-card rounded-lg p-6 max-w-md w-full mx-4 border border-border">
              <h3 className="text-lg font-semibold text-foreground mb-4">
                Cambiar Rol de Usuario
              </h3>
              <p className="text-muted-foreground mb-4">
                Cambiando rol de: <strong>{roleChangeTarget.name}</strong>
              </p>
              <div className="mb-4">
                <label className="block text-sm font-medium text-foreground mb-2">
                  Nuevo Rol
                </label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value)}
                  className="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <option value="USER">Usuario</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
              <div className="flex justify-end space-x-3">
                <button
                  onClick={() => {
                    setShowRoleChangeModal(false);
                    setRoleChangeTarget(null);
                    setNewRole('');
                  }}
                  className="px-6 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors font-medium"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleConfirmRoleChange}
                  disabled={roleChangeLoading || newRole === roleChangeTarget.currentRole}
                  className="px-6 py-2 bg-primary hover:bg-primary/90 disabled:bg-primary/50 text-primary-foreground rounded-md transition-colors flex items-center"
                >
                  {roleChangeLoading ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-foreground mr-2"></div>
                      Cambiando...
                    </>
                  ) : (
                    'Cambiar Rol'
                  )}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Modal para detalles de la orden */}
        {showOrderDetailModal && selectedOrder && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-card p-6 rounded-lg shadow-lg max-w-3xl w-full max-h-[90vh] overflow-y-auto border border-border">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-semibold text-foreground">
                  Detalles del Pedido #{selectedOrder.id}
                </h3>
                <button
                  onClick={() => setShowOrderDetailModal(false)}
                  className="text-muted-foreground hover:text-foreground"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M18 6 6 18"></path>
                    <path d="m6 6 12 12"></path>
                  </svg>
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                <div className="space-y-3">
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Cliente</p>
                    <p className="text-foreground">{selectedOrder.user?.firstName} {selectedOrder.user?.lastName}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Email</p>
                    <p className="text-foreground">{selectedOrder.user?.email}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Fecha</p>
                    <p className="text-foreground">{new Date(selectedOrder.createdAt).toLocaleString()}</p>
                  </div>
                </div>

                <div className="space-y-3">
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Estado</p>
                    <div className="mt-1">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${selectedOrder.status === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                        selectedOrder.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                          'bg-red-100 text-red-800'
                        }`}>
                        {selectedOrder.status}
                      </span>
                    </div>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Dirección de envío</p>
                    <p className="text-foreground">{selectedOrder.shippingAddress}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Total</p>
                    <p className="text-xl font-bold text-foreground">${(selectedOrder.items?.reduce((total, item) => total + ((item.price || 0) * (item.quantity || 0)), 0) || 0).toFixed(2)}</p>
                  </div>
                </div>
              </div>

              <h4 className="text-lg font-medium text-foreground mb-4">Productos</h4>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-muted">
                    <tr>
                      <th className="px-4 py-2 text-left text-sm font-medium text-muted-foreground">Producto</th>
                      <th className="px-4 py-2 text-left text-sm font-medium text-muted-foreground">Precio</th>
                      <th className="px-4 py-2 text-left text-sm font-medium text-muted-foreground">Cantidad</th>
                      <th className="px-4 py-2 text-right text-sm font-medium text-muted-foreground">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {selectedOrder.items?.map((item) => (
                      <tr key={item.id} className="hover:bg-muted/50">
                        <td className="px-4 py-3 text-foreground">
                          <div className="flex items-center gap-3">
                            {item.product.imageUrl && (
                              <div className="h-10 w-10 rounded-md bg-muted overflow-hidden">
                                <img
                                  src={item.product.imageUrl}
                                  alt={item.product.name}
                                  className="h-full w-full object-cover"
                                />
                              </div>
                            )}
                            <span>{item.product.name}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-foreground">${(item.price || 0).toFixed(2)}</td>
                        <td className="px-4 py-3 text-foreground">{item.quantity}</td>
                        <td className="px-4 py-3 text-right text-foreground">${((item.price || 0) * (item.quantity || 0)).toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mt-6 text-right space-y-2 border-t border-border pt-4">
                {(() => {
                  const calculatedSubtotal = selectedOrder.items?.reduce((total, item) =>
                    total + ((item.price || 0) * (item.quantity || 0)), 0) || 0;
                  return (
                    <>
                      <div className="flex justify-end">
                        <span className="text-muted-foreground mr-4">Subtotal:</span>
                        <span className="text-foreground">${calculatedSubtotal.toFixed(2)}</span>
                      </div>
                      <div className="flex justify-end">
                        <span className="text-muted-foreground mr-4">Envío:</span>
                        <span className="text-green-500">Gratis</span>
                      </div>
                      <div className="flex justify-end">
                        <span className="text-muted-foreground mr-4 font-medium">Total:</span>
                        <span className="text-foreground font-bold">${calculatedSubtotal.toFixed(2)}</span>
                      </div>
                    </>
                  );
                })()}
              </div>

              <div className="flex justify-end mt-8">
                <button
                  onClick={() => setShowOrderDetailModal(false)}
                  className="px-6 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors"
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Variant Attribute Modal */}
        {showVariantAttributeModal && (
          <div className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-start justify-center z-50 p-4 overflow-y-auto">
            <div className="bg-card rounded-lg shadow-xl max-w-2xl w-full mt-8 mb-8 border border-border">
              <div className="p-6">
                <h3 className="text-lg font-medium text-foreground mb-4">
                  {editingVariantAttribute ? 'Editar Atributo de Variante' : 'Crear Atributo de Variante'}
                </h3>
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Nombre Interno *
                      </label>
                      <input
                        type="text"
                        value={editingVariantAttribute ? editingVariantAttribute.name : newVariantAttribute.name}
                        onChange={(e) => {
                          const value = e.target.value;
                          if (editingVariantAttribute) {
                            setEditingVariantAttribute({ ...editingVariantAttribute, name: value });
                          } else {
                            setNewVariantAttribute({ ...newVariantAttribute, name: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="color, size, material"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Nombre para Mostrar *
                      </label>
                      <input
                        type="text"
                        value={editingVariantAttribute ? editingVariantAttribute.displayName : newVariantAttribute.displayName}
                        onChange={(e) => {
                          const value = e.target.value;
                          if (editingVariantAttribute) {
                            setEditingVariantAttribute({ ...editingVariantAttribute, displayName: value });
                          } else {
                            setNewVariantAttribute({ ...newVariantAttribute, displayName: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="Color, Talle, Material"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Descripción
                    </label>
                    <textarea
                      value={editingVariantAttribute ? (editingVariantAttribute.description || '') : newVariantAttribute.description}
                      onChange={(e) => {
                        const value = e.target.value;
                        if (editingVariantAttribute) {
                          setEditingVariantAttribute({ ...editingVariantAttribute, description: value });
                        } else {
                          setNewVariantAttribute({ ...newVariantAttribute, description: value });
                        }
                      }}
                      rows={2}
                      className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                      placeholder="Descripción opcional del atributo"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Tipo de Atributo *
                      </label>
                      <select
                        value={editingVariantAttribute ? editingVariantAttribute.attributeType : newVariantAttribute.attributeType}
                        onChange={(e) => {
                          const value = e.target.value as 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN';
                          if (editingVariantAttribute) {
                            setEditingVariantAttribute({ ...editingVariantAttribute, attributeType: value });
                          } else {
                            setNewVariantAttribute({ ...newVariantAttribute, attributeType: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        required
                      >
                        <option value="TEXT">Texto</option>
                        <option value="NUMBER">Número</option>
                        <option value="SELECT">Selección</option>
                        <option value="BOOLEAN">Verdadero/Falso</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Unidad (opcional)
                      </label>
                      <input
                        type="text"
                        value={editingVariantAttribute ? (editingVariantAttribute.unit || '') : newVariantAttribute.unit}
                        onChange={(e) => {
                          const value = e.target.value;
                          if (editingVariantAttribute) {
                            setEditingVariantAttribute({ ...editingVariantAttribute, unit: value });
                          } else {
                            setNewVariantAttribute({ ...newVariantAttribute, unit: value });
                          }
                        }}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="kg, cm, ml, etc."
                      />
                    </div>
                  </div>

                  {/* {(editingVariantAttribute ? editingVariantAttribute.attributeType : newVariantAttribute.attributeType) === 'SELECT' && (
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Opciones (una por línea)
                      </label>
                      <textarea
                        value={editingVariantAttribute ? (editingVariantAttribute.options?.join('\n') || '') : newVariantAttribute.options?.join('\n') || ''}
                        onChange={(e) => {
                          const value = e.target.value.split('\n').filter(opt => opt.trim());
                          if (editingVariantAttribute) {
                            setEditingVariantAttribute({ ...editingVariantAttribute, options: value });
                          } else {
                            setNewVariantAttribute({ ...newVariantAttribute, options: value });
                          }
                        }}
                        rows={4}
                        className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                        placeholder="Rojo&#10;Azul&#10;Verde&#10;Amarillo"
                      />
                    </div>
                  )} */}

                  <div className="flex items-center space-x-4">
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={editingVariantAttribute ? editingVariantAttribute.required : newVariantAttribute.required}
                        onChange={(e) => {
                          const checked = e.target.checked;
                          if (editingVariantAttribute) {
                            setEditingVariantAttribute({ ...editingVariantAttribute, required: checked });
                          } else {
                            setNewVariantAttribute({ ...newVariantAttribute, required: checked });
                          }
                        }}
                        className="rounded border-border text-primary focus:ring-primary"
                      />
                      <span className="ml-2 text-sm text-foreground">Requerido</span>
                    </label>
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={editingVariantAttribute ? editingVariantAttribute.active : true}
                        onChange={(e) => {
                          const checked = e.target.checked;
                          if (editingVariantAttribute) {
                            setEditingVariantAttribute({ ...editingVariantAttribute, active: checked });
                          }
                        }}
                        className="rounded border-border text-primary focus:ring-primary"
                      />
                      <span className="ml-2 text-sm text-foreground">Activo</span>
                    </label>
                  </div>

                  {/* Alcance del atributo */}
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-3">
                      Alcance del Atributo
                    </label>
                    <div className="space-y-3">
                      <label className="flex items-center">
                        <input
                          type="radio"
                          name="attributeScope"
                          checked={editingVariantAttribute ? editingVariantAttribute.global : newVariantAttribute.global}
                          onChange={() => {
                            if (editingVariantAttribute) {
                              setEditingVariantAttribute({ ...editingVariantAttribute, global: true });
                            } else {
                              setNewVariantAttribute({ ...newVariantAttribute, global: true });
                            }
                            setSelectedCategories([]); // Limpiar categorías seleccionadas cuando se cambia a global
                          }}
                          className="text-primary focus:ring-primary"
                        />
                        <span className="ml-2 text-sm text-foreground">
                          <strong>Global</strong> - Disponible para todas las categorías
                        </span>
                      </label>
                      <label className="flex items-center">
                        <input
                          type="radio"
                          name="attributeScope"
                          checked={editingVariantAttribute ? !editingVariantAttribute.global : !newVariantAttribute.global}
                          onChange={() => {
                            if (editingVariantAttribute) {
                              setEditingVariantAttribute({ ...editingVariantAttribute, global: false });
                            } else {
                              setNewVariantAttribute({ ...newVariantAttribute, global: false });
                            }
                          }}
                          className="text-primary focus:ring-primary"
                        />
                        <span className="ml-2 text-sm text-foreground">
                          <strong>Específico por categoría</strong> - Solo para categorías seleccionadas
                        </span>
                      </label>
                    </div>
                  </div>

                  {/* Selector de categorías (solo si no es global) */}
                  {!(editingVariantAttribute ? editingVariantAttribute.global : newVariantAttribute.global) && (
                    <div>
                      <label className="block text-sm font-medium text-foreground mb-2">
                        Categorías donde aplicar este atributo *
                      </label>
                      <div className="border border-border rounded-md p-3 max-h-40 overflow-y-auto bg-background">
                        {categories.length === 0 ? (
                          <p className="text-sm text-muted-foreground">Cargando categorías...</p>
                        ) : (
                          categories
                            .filter(cat => cat.active)
                            .map(category => (
                              <label key={category.id} className="flex items-center py-1">
                                <input
                                  type="checkbox"
                                  checked={selectedCategories.includes(Number(category.id))}
                                  onChange={(e) => {
                                    const checked = e.target.checked;
                                    if (checked) {
                                      setSelectedCategories([...selectedCategories, Number(category.id)]);
                                    } else {
                                      setSelectedCategories(selectedCategories.filter(id => id !== Number(category.id)));
                                    }
                                  }}
                                  className="rounded border-border text-primary focus:ring-primary mr-2"
                                />
                                <span className="text-sm text-foreground">{category.name}</span>
                              </label>
                            ))
                        )}
                      </div>
                      {selectedCategories.length === 0 && (
                        <p className="text-xs text-destructive mt-1">
                          Debe seleccionar al menos una categoría
                        </p>
                      )}
                    </div>
                  )}
                </div>

                <div className="flex justify-end space-x-3 mt-6">
                  <button
                    onClick={() => {
                      setShowVariantAttributeModal(false);
                      setEditingVariantAttribute(null);
                    }}
                    className="px-4 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={editingVariantAttribute ? handleUpdateVariantAttribute : handleCreateVariantAttribute}
                    className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
                  >
                    {editingVariantAttribute ? 'Actualizar' : 'Crear'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Hide Review Modal */}
        {showHideReviewModal && (
          <div className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-card rounded-lg shadow-xl max-w-md w-full border border-border">
              <div className="p-6">
                <h3 className="text-lg font-medium text-foreground mb-4">
                  Ocultar Reseña
                </h3>
                <p className="text-sm text-muted-foreground mb-4">
                  Reseña del producto: <strong>{reviewToHide?.productName}</strong>
                </p>
                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Razón para ocultar *
                  </label>
                  <textarea
                    value={hideReviewReason}
                    onChange={(e) => setHideReviewReason(e.target.value)}
                    placeholder="Por ejemplo: Contiene lenguaje ofensivo, información no verificable, etc."
                    maxLength={500}
                    className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background resize-none"
                    rows={4}
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    {hideReviewReason.length}/500 caracteres
                  </p>
                </div>
                <div className="flex justify-end space-x-3 mt-6">
                  <button
                    onClick={() => {
                      setShowHideReviewModal(false);
                      setReviewToHide(null);
                      setHideReviewReason('');
                    }}
                    className="px-4 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors"
                    disabled={hidingReview}
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={handleHideReviewSubmit}
                    className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                    disabled={hidingReview}
                  >
                    {hidingReview ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                        <span>Ocultando...</span>
                      </>
                    ) : (
                      'Ocultar Reseña'
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Admin;


import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { publicAPI, productAPI, adminReviewAPI } from '../services/api';
import { useCartStore } from '../stores/cartStore';
import { useUserStore } from '../stores/userStore';
import { useNotificationStore } from '../stores/notificationStore';
import SignedImage from '../components/SignedImage';
import { ShoppingCart, Zap, MessageCircle } from 'lucide-react';

interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  sku: string;
  imageUrl: string;
  category?: {
    id: string;
    name: string;
    description: string;
  };
  createdAt: string;
  updatedAt: string;
}

interface Review {
  id: number;
  productId: number;
  productName: string;
  rating: number;
  title?: string;
  comment?: string;
  imageUrls: string[];
  verifiedPurchase: boolean;
  approved: boolean;
  createdAt: string;
  approvedAt?: string;
  user: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    role: string;
  };
}

const ProductDetail = () => {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [editingReviewId, setEditingReviewId] = useState<number | null>(null);
  const [editingReview, setEditingReview] = useState({
    rating: 5,
    comment: '',
    title: ''
  });
  const [newReview, setNewReview] = useState({
    rating: 5,
    comment: '',
    title: ''
  });
  const [submittingReview, setSubmittingReview] = useState(false);
  const [showHideReviewModal, setShowHideReviewModal] = useState(false);
  const [reviewToHide, setReviewToHide] = useState<number | null>(null);
  const [hideReviewReason, setHideReviewReason] = useState('');
  const [hidingReview, setHidingReview] = useState(false);
  const { addItem } = useCartStore();
  const { user } = useUserStore();
  const { addNotification } = useNotificationStore();

  useEffect(() => {
    const fetchProductAndReviews = async () => {
      if (!id) return;
      try {
        const response = await publicAPI.getProduct(parseInt(id));
        // La respuesta incluye tanto el producto como las reseñas
        // El backend retorna: { product: {...}, reviews: [...] }
        // Intentar acceder a los datos de forma segura
        const productData = response.data.product || response.data;
        const reviewsData = response.data.reviews || [];

        if (productData) {
          setProduct(productData);
          setReviews(reviewsData);
        }
      } catch (error: any) {
        // Error handling - silencioso en producción
      } finally {
        setLoading(false);
        setReviewsLoading(false);
      }
    };

    fetchProductAndReviews();
  }, [id]);

  const handleAddToCart = async () => {
    if (!product) return;

    if (product.stockQuantity === 0) {
      addNotification({
        type: 'error',
        message: 'Este producto no tiene stock disponible',
        duration: 4000
      });
      return;
    }

    try {
      // Logging removed for production mode
      await addItem({
        id: product.id,
        name: product.name,
        price: product.price,
        quantity,
      });
      // Success logging removed for production mode
      addNotification({
        type: 'success',
        message: 'Producto agregado al carrito exitosamente',
        duration: 3000
      });
    } catch (error) {
      // Error handling removed for production mode
      addNotification({
        type: 'error',
        message: 'Error al agregar producto al carrito',
        duration: 4000
      });
    }
  };

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !user) return;

    // Validación frontend
    if (!newReview.title || newReview.title.trim().length < 3) {
      addNotification({
        type: 'error',
        message: 'El título debe tener al menos 3 caracteres',
        duration: 4000
      });
      return;
    }
    if (!newReview.comment || newReview.comment.trim().length < 10) {
      addNotification({
        type: 'error',
        message: 'El comentario debe tener al menos 10 caracteres',
        duration: 4000
      });
      return;
    }

    try {
      setSubmittingReview(true);
      await productAPI.createProductReview(parseInt(id), {
        rating: newReview.rating,
        comment: newReview.comment.trim(),
        title: newReview.title.trim(),
      });

      // Reset form
      setNewReview({ rating: 5, comment: '', title: '' });
      setShowReviewForm(false);

      // Reload reviews
      const response = await publicAPI.getProductReviews(parseInt(id));
      setReviews(response.data.content || response.data);
    } catch (error) {
      // Error handling removed for production mode
      addNotification({
        type: 'error',
        message: 'Error al enviar la reseña. Por favor intenta de nuevo.',
      });
      //alert('Error al enviar la reseña. Por favor intenta de nuevo.');
    } finally {
      setSubmittingReview(false);
    }
  };

  const handleEditReview = (review: Review) => {
    // console.log('handleEditReview called with review:', review);
    setEditingReviewId(review.id);
    setEditingReview({
      rating: review.rating,
      comment: review.comment || '',
      title: review.title || ''
    });
  };

  const handleCancelEdit = () => {
    setEditingReviewId(null);
    setEditingReview({ rating: 5, comment: '', title: '' });
  };

  const handleUpdateReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !user || !editingReviewId) return;

    // Validación frontend
    if (!editingReview.title || editingReview.title.trim().length < 3) {
      addNotification({
        type: 'warning',
        message: 'El título debe tener al menos 3 caracteres',
      });
      //alert('El título debe tener al menos 3 caracteres');
      return;
    }
    if (!editingReview.comment || editingReview.comment.trim().length < 10) {
      addNotification({
        type: 'warning',
        message: 'El comentario debe tener al menos 10 caracteres',
      });
      //alert('El comentario debe tener al menos 10 caracteres');
      return;
    }

    setSubmittingReview(true);
    // console.log('Calling updateProductReview with:', {
    //   productId: parseInt(id),
    //   reviewId: editingReviewId,
    //   data: {
    //     rating: editingReview.rating,
    //     comment: editingReview.comment.trim(),
    //     title: editingReview.title.trim(),
    //   }
    // });
    try {
      await productAPI.updateProductReview(parseInt(id), editingReviewId, {
        productId: parseInt(id),
        rating: editingReview.rating,
        comment: editingReview.comment.trim(),
        title: editingReview.title.trim(),
      });

      // Reset form
      setEditingReviewId(null);
      setEditingReview({ rating: 5, comment: '', title: '' });

      // Reload reviews
      const response = await publicAPI.getProduct(parseInt(id));
      if (response.data && response.data.product) {
        setProduct(response.data.product);
        setReviews(response.data.reviews || []);
      }
    } catch (error: any) {
      // Error handling removed for production mode
      addNotification({
        type: 'error',
        message: `Error al actualizar la reseña: ${error.response?.data?.message || error.message || 'Error desconocido'}`,
      });
      //alert(`Error al actualizar la reseña: ${error.response?.data?.message || error.message || 'Error desconocido'}`);
    } finally {
      setSubmittingReview(false);
    }
  };

  const handleDeleteReview = async (reviewId: number) => {
    // console.log('handleDeleteReview called with reviewId:', reviewId);
    if (!id || !confirm('¿Estás seguro de que quieres eliminar esta reseña?')) return;

    try {
      // console.log('Calling deleteProductReview with:', {
      //   productId: parseInt(id),
      //   reviewId: reviewId
      // });
      await productAPI.deleteProductReview(parseInt(id), reviewId);

      // Reload reviews
      const response = await publicAPI.getProduct(parseInt(id));
      if (response.data && response.data.product) {
        setProduct(response.data.product);
        setReviews(response.data.reviews || []);
      }
    } catch (error: any) {
      // Error handling removed for production mode
      addNotification({
        type: 'error',
        message: `Error al eliminar la reseña: ${error.response?.data?.message || error.message || 'Error desconocido'}`,
      });
      //alert(`Error al eliminar la reseña: ${error.response?.data?.message || error.message || 'Error desconocido'}`);
    }
  };

  const handleAdminDeleteReview = (reviewId: number) => {
    setReviewToHide(reviewId);
    setShowHideReviewModal(true);
    setHideReviewReason('');
  };

  const handleHideReviewSubmit = async () => {
    if (!id || !reviewToHide || !hideReviewReason.trim()) {
      addNotification({
        type: 'warning',
        message: 'Por favor proporciona una razón para ocultar la reseña',
      });
      return;
    }

    if (hideReviewReason.trim().length < 5 || hideReviewReason.trim().length > 500) {
      addNotification({
        type: 'warning',
        message: 'La razón debe tener entre 5 y 500 caracteres',
      });
      return;
    }

    setHidingReview(true);
    try {
      await adminReviewAPI.deleteReview(reviewToHide, hideReviewReason);

      addNotification({
        type: 'success',
        message: 'Reseña ocultada correctamente',
      });

      // Reload reviews
      const response = await publicAPI.getProduct(parseInt(id));
      if (response.data && response.data.product) {
        setProduct(response.data.product);
        setReviews(response.data.reviews || []);
      }

      // Reset modal
      setShowHideReviewModal(false);
      setReviewToHide(null);
      setHideReviewReason('');
    } catch (error: any) {
      addNotification({
        type: 'error',
        message: `Error al ocultar la reseña: ${error.response?.data?.message || error.message || 'Error desconocido'}`,
      });
    } finally {
      setHidingReview(false);
    }
  };


  if (loading) return (
    <div className="min-h-screen bg-background flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-primary mx-auto mb-4"></div>
        <p className="text-xl text-muted-foreground">Cargando producto...</p>
      </div>
    </div>
  );

  if (!product) return (
    <div className="min-h-screen bg-background flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-2xl font-bold text-foreground mb-2">Producto no encontrado</h1>
        <p className="text-muted-foreground">El producto que buscas no existe.</p>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4">
        <div className="bg-card rounded-lg shadow-sm overflow-hidden border border-border">
          <div className="md:flex">
            {/* Product Image */}
            <div className="md:w-1/2 p-8">
              <div className="aspect-square bg-muted rounded-lg overflow-hidden">
                <SignedImage
                  src={product.imageUrl || '/placeholder-product.jpg'}
                  alt={product.name}
                  className="w-full h-full object-cover"
                />
              </div>
            </div>

            {/* Product Info */}
            <div className="md:w-1/2 p-8">
              <h1 className="text-3xl font-bold text-foreground mb-4">{product.name}</h1>
              <p className="text-muted-foreground mb-6 text-lg">{product.description}</p>

              <div className="mb-6">
                <span className="text-4xl font-bold text-primary">${product.price}</span>
                <p className="text-sm text-muted-foreground mt-1">Precio final, sin envío</p>
                {product.stockQuantity === 0 && (
                  <p className="text-red-600 font-medium mt-2">Producto sin stock</p>
                )}
                {product.stockQuantity > 0 && product.stockQuantity <= 5 && (
                  <p className="text-orange-600 font-medium mt-2">¡Últimas unidades disponibles!</p>
                )}
              </div>

              {/* Quantity Selector */}
              <div className="mb-6">
                <label className="block text-sm font-medium text-foreground mb-2">
                  Cantidad
                </label>
                <div className="flex items-center space-x-3">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="w-10 h-10 rounded-lg border border-border flex items-center justify-center hover:bg-muted transition-colors"
                  >
                    -
                  </button>
                  <span className="w-12 text-center font-medium text-foreground">{quantity}</span>
                  <button
                    onClick={() => setQuantity(quantity + 1)}
                    disabled={quantity >= product.stockQuantity}
                    className={`w-10 h-10 rounded-lg border border-border flex items-center justify-center transition-colors ${quantity >= product.stockQuantity
                      ? 'bg-gray-200 cursor-not-allowed text-gray-400'
                      : 'hover:bg-muted'
                      }`}
                  >
                    +
                  </button>
                </div>
              </div>

              {/* Add to Cart Button */}
              <button
                onClick={handleAddToCart}
                disabled={product.stockQuantity === 0}
                className={`w-full font-bold py-4 px-6 rounded-lg transition-colors mb-4 shadow-sm hover:shadow-md flex items-center justify-center gap-2 ${product.stockQuantity === 0
                  ? 'bg-gray-400 cursor-not-allowed text-gray-200'
                  : 'bg-primary hover:bg-primary/90 text-primary-foreground'
                  }`}
              >
                <ShoppingCart className="w-5 h-5" />
                {product.stockQuantity === 0 ? 'Sin stock disponible' : 'Agregar al carrito'}
              </button>

              {/* Buy Now Button */}
              <button
                disabled={product.stockQuantity === 0}
                className={`w-full font-bold py-4 px-6 rounded-lg transition-colors shadow-sm hover:shadow-md flex items-center justify-center gap-2 ${product.stockQuantity === 0
                  ? 'bg-gray-400 cursor-not-allowed text-gray-200'
                  : 'bg-accent hover:bg-accent/90 text-accent-foreground'
                  }`}
              >
                <Zap className="w-5 h-5" />
                Comprar ahora
              </button>

              {/* Additional Info */}
              <div className="mt-8 border-t border-border pt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div className="flex items-center text-green-600">
                    <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    Envío gratis
                  </div>
                  <div className="flex items-center text-blue-600">
                    <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    Devolución gratuita
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Reviews Section */}
        <div className="mt-12 bg-card rounded-lg shadow-sm border border-border p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold text-foreground">Comentarios y Reseñas</h2>
            {user ? (
              <button
                onClick={() => setShowReviewForm(!showReviewForm)}
                className="bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2 rounded-md transition-colors"
              >
                {showReviewForm ? 'Cancelar' : 'Escribir Reseña'}
              </button>
            ) : (
              <div className="text-sm text-muted-foreground">
                <span>¿Quieres dejar un comentario? </span>
                <a href="/login" className="text-primary hover:text-primary/80 underline">
                  Inicia sesión
                </a>
                <span> o </span>
                <a href="/register" className="text-primary hover:text-primary/80 underline">
                  regístrate
                </a>
              </div>
            )}
          </div>

          {/* Review Form */}
          {showReviewForm && user && (
            <div className="mb-8 p-4 bg-muted rounded-lg">
              <h3 className="text-lg font-semibold text-foreground mb-4">Escribe tu reseña</h3>
              <form onSubmit={handleSubmitReview} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Calificación *
                  </label>
                  <div className="flex items-center space-x-2">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setNewReview({ ...newReview, rating: star })}
                        className={`text-2xl ${star <= newReview.rating ? 'text-yellow-400' : 'text-gray-300'} hover:text-yellow-400 transition-colors`}
                      >
                        ★
                      </button>
                    ))}
                    <span className="ml-2 text-sm text-muted-foreground">
                      {newReview.rating} estrella{newReview.rating !== 1 ? 's' : ''}
                    </span>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Título (opcional)
                  </label>
                  <input
                    type="text"
                    value={newReview.title}
                    onChange={(e) => setNewReview({ ...newReview, title: e.target.value })}
                    className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                    placeholder="Resumen de tu experiencia..."
                    maxLength={100}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Comentario *
                  </label>
                  <textarea
                    value={newReview.comment}
                    onChange={(e) => setNewReview({ ...newReview, comment: e.target.value })}
                    rows={4}
                    className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                    placeholder="Comparte tu experiencia con este producto..."
                    required
                  />
                </div>

                <div className="flex justify-end space-x-3">
                  <button
                    type="button"
                    onClick={() => setShowReviewForm(false)}
                    className="px-4 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={submittingReview || !newReview.comment.trim()}
                    className="px-4 py-2 bg-primary hover:bg-primary/90 disabled:bg-primary/50 text-primary-foreground rounded-md transition-colors flex items-center"
                  >
                    {submittingReview ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-foreground mr-2"></div>
                        Enviando...
                      </>
                    ) : (
                      'Enviar Reseña'
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Reviews List */}
          <div className="space-y-6">
            {reviewsLoading ? (
              <div className="flex justify-center items-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                <span className="ml-2 text-muted-foreground">Cargando comentarios...</span>
              </div>
            ) : reviews.length === 0 ? (
              <div className="text-center py-8">
                <MessageCircle className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
                <h3 className="text-lg font-medium text-foreground mb-2">No hay comentarios aún</h3>
                <p className="text-muted-foreground mb-4">
                  Sé el primero en compartir tu opinión sobre este producto.
                </p>
                {user ? (
                  <button
                    onClick={() => setShowReviewForm(true)}
                    className="bg-primary hover:bg-primary/90 text-primary-foreground px-6 py-2 rounded-md transition-colors"
                  >
                    Escribir la primera reseña
                  </button>
                ) : (
                  <div className="text-sm text-muted-foreground">
                    <a href="/login" className="text-primary hover:text-primary/80 underline">
                      Inicia sesión
                    </a>
                    <span> para dejar el primer comentario</span>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                {reviews.map((review) => (
                  <div key={review.id} className="border border-border rounded-lg p-4 bg-background">
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-primary rounded-full flex items-center justify-center text-primary-foreground font-semibold">
                          {(review.user.firstName || review.user.email).charAt(0).toUpperCase()}
                          {(review.user.lastName || '').charAt(0).toUpperCase() || (review.user.email.charAt(1) || '').toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-foreground">
                            {review.user.firstName && review.user.lastName
                              ? `${review.user.firstName} ${review.user.lastName}`
                              : review.user.email.split('@')[0]
                            }
                          </p>
                          <div className="flex items-center space-x-2">
                            <div className="flex items-center">
                              {[1, 2, 3, 4, 5].map((star) => (
                                <span
                                  key={star}
                                  className={`text-sm ${star <= review.rating ? 'text-yellow-400' : 'text-gray-300'}`}
                                >
                                  ★
                                </span>
                              ))}
                            </div>
                            <span className="text-xs text-muted-foreground">
                              {new Date(review.createdAt).toLocaleDateString()}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Botones de editar/eliminar para reseñas propias y eliminar para admins */}
                      {(() => {
                        const isOwnReview = user && Number(review.user.id) === Number(user.id);
                        const isAdmin = user && user.role === 'ADMIN';

                        return (
                          <div className="flex space-x-2">
                            {isOwnReview && (
                              <>
                                <button
                                  onClick={() => handleEditReview(review)}
                                  className="text-xs px-2 py-1 bg-blue-500 hover:bg-blue-600 text-white rounded transition-colors"
                                >
                                  Editar
                                </button>
                                <button
                                  onClick={() => handleDeleteReview(review.id)}
                                  className="text-xs px-2 py-1 bg-red-500 hover:bg-red-600 text-white rounded transition-colors"
                                >
                                  Eliminar
                                </button>
                              </>
                            )}
                            {isAdmin && !isOwnReview && (
                              <button
                                onClick={() => handleAdminDeleteReview(review.id)}
                                className="text-xs px-2 py-1 bg-red-600 hover:bg-red-700 text-white rounded transition-colors"
                              >
                                Ocultar Reseña
                              </button>
                            )}
                          </div>
                        );
                      })()}
                    </div>

                    {/* Formulario de edición o contenido de reseña */}
                    {editingReviewId === review.id ? (
                      <form onSubmit={handleUpdateReview} className="space-y-4">
                        <div>
                          <label className="block text-sm font-medium text-foreground mb-2">
                            Calificación
                          </label>
                          <div className="flex space-x-1">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <button
                                key={star}
                                type="button"
                                onClick={() => setEditingReview({ ...editingReview, rating: star })}
                                className={`text-2xl ${star <= editingReview.rating ? 'text-yellow-400' : 'text-gray-300'} hover:text-yellow-400 transition-colors`}
                              >
                                ★
                              </button>
                            ))}
                          </div>
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-foreground mb-2">
                            Título
                          </label>
                          <input
                            type="text"
                            value={editingReview.title}
                            onChange={(e) => setEditingReview({ ...editingReview, title: e.target.value })}
                            className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                            placeholder="Título de tu reseña..."
                            required
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-foreground mb-2">
                            Comentario
                          </label>
                          <textarea
                            value={editingReview.comment}
                            onChange={(e) => setEditingReview({ ...editingReview, comment: e.target.value })}
                            rows={4}
                            className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                            placeholder="Comparte tu experiencia con este producto..."
                            required
                          />
                        </div>

                        <div className="flex justify-end space-x-3">
                          <button
                            type="button"
                            onClick={handleCancelEdit}
                            className="px-4 py-2 border border-border rounded-md text-foreground hover:bg-muted transition-colors"
                          >
                            Cancelar
                          </button>
                          <button
                            type="submit"
                            disabled={submittingReview || !editingReview.comment.trim()}
                            className="px-4 py-2 bg-primary hover:bg-primary/90 disabled:bg-primary/50 text-primary-foreground rounded-md transition-colors flex items-center"
                          >
                            {submittingReview ? (
                              <>
                                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-foreground mr-2"></div>
                                Actualizando...
                              </>
                            ) : (
                              'Actualizar Reseña'
                            )}
                          </button>
                        </div>
                      </form>
                    ) : (
                      <>
                        {review.title && (
                          <h4 className="font-medium text-foreground mb-2">{review.title}</h4>
                        )}

                        {review.comment && (
                          <p className="text-muted-foreground">{review.comment}</p>
                        )}
                      </>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Admin Hide Review Modal */}
        {showHideReviewModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-card rounded-lg p-6 w-full max-w-md shadow-lg border border-border">
              <h3 className="text-lg font-semibold text-foreground mb-4">Ocultar Reseña</h3>
              <div className="mb-4">
                <label className="block text-sm font-medium text-foreground mb-2">
                  Razón para ocultar (5-500 caracteres)
                </label>
                <textarea
                  value={hideReviewReason}
                  onChange={(e) => setHideReviewReason(e.target.value)}
                  rows={4}
                  className="w-full px-3 py-2 border border-border rounded-md focus:ring-2 focus:ring-ring focus:border-transparent bg-background text-foreground"
                  placeholder="Ej: Contenido inapropiado, lenguaje ofensivo, etc..."
                />
                <p className="text-xs text-muted-foreground mt-1">
                  {hideReviewReason.length}/500
                </p>
              </div>

              <div className="flex justify-end space-x-3">
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
                  disabled={hidingReview || hideReviewReason.trim().length < 5}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-red-600/50 text-white rounded-md transition-colors flex items-center"
                >
                  {hidingReview ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                      Ocultando...
                    </>
                  ) : (
                    'Ocultar Reseña'
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductDetail;

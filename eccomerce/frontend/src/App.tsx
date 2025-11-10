import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';
import CartRoute from './components/CartRoute';
import ScrollToTop from './components/ScrollToTop';
import NotificationContainer from './components/NotificationContainer';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import VerifyEmail from './pages/VerifyEmail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Orders from './pages/Orders';
import Admin from './pages/Admin';

import Profile from './pages/Profile';
import ProfileEdit from './pages/ProfileEdit';
import Addresses from './pages/Addresses';
import SearchResults from './pages/SearchResults';
import Products from './pages/Products';
import Categories from './pages/Categories';
import ProductDetail from './pages/ProductDetail';
import Success from './pages/Success';
import Failure from './pages/Failure';
import Pending from './pages/Pending';


function App() {
  // console.log('App: Componente renderizado');
  return (
    <Router>
      <ScrollToTop />
      <NotificationContainer />
      <div className="min-h-screen flex flex-col">
        <Navbar />
        <main className="flex-grow">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/products" element={<Products />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/product/:id" element={<ProductDetail />} />
            <Route path="/search" element={<SearchResults />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/verify-email" element={<VerifyEmail />} />
            <Route path="/cart" element={
              <CartRoute>
                <Cart />
              </CartRoute>
            } />
            <Route path="/checkout" element={
              <ProtectedRoute>
                <Checkout />
              </ProtectedRoute>
            } />
            <Route path="/orders" element={
              <ProtectedRoute>
                <Orders />
              </ProtectedRoute>
            } />
            <Route path="/admin" element={
              <ProtectedRoute requireAdmin={true}>
                <Admin />
              </ProtectedRoute>
            } />
            <Route path="/profile" element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            } />
            <Route path="/profile/edit" element={
              <ProtectedRoute>
                <ProfileEdit />
              </ProtectedRoute>
            } />
            <Route path="/addresses" element={
              <ProtectedRoute>
                <Addresses />
              </ProtectedRoute>
            } />
            <Route path="/success" element={<Success />} />
            <Route path="/failure" element={<Failure />} />
            <Route path="/pending" element={<Pending />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  );
}

export default App;

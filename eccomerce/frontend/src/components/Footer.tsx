import React from 'react';
import { Link } from 'react-router-dom';
import { Facebook, Twitter, Instagram, Github } from 'lucide-react';

const Footer: React.FC = () => {
  return (
    <footer className="bg-muted/30 border-t border-border">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand */}
          <div className="space-y-4">
            <div className="flex items-center space-x-2">
              <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center">
                <span className="text-primary-foreground font-bold text-lg">E</span>
              </div>
              <span className="font-bold text-xl text-foreground">EcomStore</span>
            </div>
            <p className="text-muted-foreground text-sm">
              Your trusted partner for premium products and exceptional shopping experience.
            </p>
            <div className="flex space-x-4">
              <Link to="#" className="text-muted-foreground hover:text-foreground transition-colors">
                <Facebook className="h-5 w-5" />
              </Link>
              <Link to="#" className="text-muted-foreground hover:text-foreground transition-colors">
                <Twitter className="h-5 w-5" />
              </Link>
              <Link to="#" className="text-muted-foreground hover:text-foreground transition-colors">
                <Instagram className="h-5 w-5" />
              </Link>
              <Link to="#" className="text-muted-foreground hover:text-foreground transition-colors">
                <Github className="h-5 w-5" />
              </Link>
            </div>
          </div>

          {/* Quick Links */}
          <div className="space-y-4">
            <h3 className="font-semibold text-foreground">Quick Links</h3>
            <div className="space-y-2">
              <Link
                to="/products"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Products
              </Link>
              <Link
                to="/categories"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Categories
              </Link>
              <Link
                to="/about"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                About Us
              </Link>
              <Link
                to="/contact"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Contact
              </Link>
            </div>
          </div>

          {/* Customer Service */}
          <div className="space-y-4">
            <h3 className="font-semibold text-foreground">Customer Service</h3>
            <div className="space-y-2">
              <Link
                to="/help"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Help Center
              </Link>
              <Link
                to="/shipping"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Shipping Info
              </Link>
              <Link
                to="/returns"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Returns
              </Link>
              <Link
                to="/privacy"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Privacy Policy
              </Link>
            </div>
          </div>

          {/* Account */}
          <div className="space-y-4">
            <h3 className="font-semibold text-foreground">Account</h3>
            <div className="space-y-2">
              <Link
                to="/login"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Register
              </Link>
              <Link
                to="/profile"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                My Account
              </Link>
              <Link
                to="/orders"
                className="block text-muted-foreground hover:text-foreground transition-colors text-sm"
              >
                Order History
              </Link>
            </div>
          </div>
        </div>

        <div className="border-t border-border mt-8 pt-8 text-center">
          <p className="text-muted-foreground text-sm">© 2025 EcomStore. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;

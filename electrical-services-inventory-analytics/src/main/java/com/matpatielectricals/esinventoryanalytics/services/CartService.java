package com.matpatielectricals.esinventoryanalytics.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matpatielectricals.esinventoryanalytics.entities.Cart;
import com.matpatielectricals.esinventoryanalytics.entities.Product;
import com.matpatielectricals.esinventoryanalytics.entities.User;
import com.matpatielectricals.esinventoryanalytics.repositories.CartRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductRepository;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    public void addToCart(User user, Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        
        if (product != null) {
            // Check if product is already in cart
            Cart existingCartItem = cartRepository.findByUserAndProduct(user, product);

            if (existingCartItem != null) {
                // Item exists, increase quantity
                existingCartItem.setQuantity(existingCartItem.getQuantity() + 1);
                cartRepository.save(existingCartItem);
            } else {
                // New item, add to cart
                Cart newCartItem = new Cart(user, product, 1);
                cartRepository.save(newCartItem);
            }
        }
    }
public void addToCart(User user, Long productId, int quantity) {
        
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        // Check if product is already in cart
        Cart cartItem = cartRepository.findByUserAndProduct(user, product);

        if (cartItem != null) {
            // Add new quantity to existing
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            // New entry
            cartItem = new Cart();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
        }

        cartRepository.save(cartItem);
    }
}
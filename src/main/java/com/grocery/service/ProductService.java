package com.grocery.service;

import com.grocery.dto.ProductDto;
import com.grocery.entity.Inventory;
import com.grocery.entity.Product;
import com.grocery.exception.ResourceNotFoundException;
import com.grocery.repository.InventoryRepository;
import com.grocery.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    // ─── Get All ───────────────────────────────────────────────────────────────
    public List<ProductDto.Response> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Get By ID ─────────────────────────────────────────────────────────────
    public ProductDto.Response getProductById(Long id) {
        Product product = findProductById(id);
        return toResponse(product);
    }

    // ─── Search ────────────────────────────────────────────────────────────────
    public List<ProductDto.Response> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Get By Category ───────────────────────────────────────────────────────
    public List<ProductDto.Response> getByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Create ────────────────────────────────────────────────────────────────
    public ProductDto.Response createProduct(ProductDto.Request request) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .stockQuantity(request.getStockQuantity())
                .build();

        product = productRepository.save(product);

        // Auto-create inventory entry when product is created
        Inventory inventory = Inventory.builder()
                .product(product)
                .quantity(product.getStockQuantity())
                .threshold(10)
                .build();
        inventoryRepository.save(inventory);

        return toResponse(product);
    }

    // ─── Update ────────────────────────────────────────────────────────────────
    public ProductDto.Response updateProduct(Long id, ProductDto.Request request) {
        Product product = findProductById(id);

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        product.setStockQuantity(request.getStockQuantity());

        // Sync inventory quantity
        inventoryRepository.findByProductId(id).ifPresent(inv -> {
            inv.setQuantity(request.getStockQuantity());
            inventoryRepository.save(inv);
        });

        return toResponse(productRepository.save(product));
    }

    // ─── Delete ────────────────────────────────────────────────────────────────
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        inventoryRepository.findByProductId(id).ifPresent(inventoryRepository::delete);
        productRepository.delete(product);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────
    public Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductDto.Response toResponse(Product product) {
        boolean lowStock = product.getStockQuantity() <= 10;
        return new ProductDto.Response(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                product.getDescription(),
                product.getStockQuantity(),
                lowStock
        );
    }
}

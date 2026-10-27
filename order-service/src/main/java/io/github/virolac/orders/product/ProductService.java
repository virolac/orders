package io.github.virolac.orders.product;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(ProductCacheConfig.PRODUCTS_CACHE)
    public ProductResponse getProductById(Long id) {
        return productRepository
                .findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = new Product(
                productRequest.name(),
                productRequest.price(),
                productRequest.description(),
                productRequest.imageUrl()
        );

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CachePut(cacheNames = ProductCacheConfig.PRODUCTS_CACHE, key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(productRequest.name());
        product.setPrice(productRequest.price());
        product.setDescription(productRequest.description());
        product.setImageUrl(productRequest.imageUrl());

        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = ProductCacheConfig.PRODUCTS_CACHE, key = "#id")
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProductInUseException(id);
        }
    }
}

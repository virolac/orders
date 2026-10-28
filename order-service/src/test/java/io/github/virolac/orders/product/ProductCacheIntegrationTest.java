package io.github.virolac.orders.product;

import io.github.virolac.orders.RabbitMQTestcontainersConfiguration;
import io.github.virolac.orders.RedisTestcontainersConfiguration;
import io.github.virolac.orders.TestcontainersConfiguration;
import io.github.virolac.orders.order.OrderItemRequest;
import io.github.virolac.orders.order.OrderService;
import io.github.virolac.orders.order.PlaceOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({
        TestcontainersConfiguration.class,
        RabbitMQTestcontainersConfiguration.class,
        RedisTestcontainersConfiguration.class
})
class ProductCacheIntegrationTest {
    private static final String DESCRIPTION = "A soft blush.";
    private static final String IMAGE_URL = "https://cdn.example.com/blush.webp";
    private static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Cache productsCache;

    @BeforeEach
    void clearCache() {
        productsCache = cacheManager.getCache(ProductCacheConfig.PRODUCTS_CACHE);
        if (productsCache != null) {
            productsCache.clear();
        }
    }

    @Test
    void secondReadIsServedFromCache() {
        Long id = createProduct("Cached Blush", "5.00");
        productService.getProductById(id);

        jdbcTemplate.update("UPDATE product SET price = ? WHERE id = ?", new BigDecimal("6.00"), id);

        assertThat(productService.getProductById(id).price()).isEqualByComparingTo("5.00");
    }

    @Test
    void cachedValueIncludesAllFields() {
        Long id = createProduct("Complete Blush", "5.00");
        productService.getProductById(id);

        ProductResponse cached = productsCache.get(id, ProductResponse.class);

        assertThat(cached).isNotNull();
        assertThat(cached.name()).isEqualTo("Complete Blush");
        assertThat(cached.description()).isEqualTo(DESCRIPTION);
        assertThat(cached.imageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void updateReplacesCachedValue() {
        Long id = createProduct("Updated Blush", "5.00");
        productService.getProductById(id);

        productService.updateProduct(id, new ProductRequest("Updated Blush", new BigDecimal("7.00"), DESCRIPTION, IMAGE_URL));

        ProductResponse cached = productsCache.get(id, ProductResponse.class);
        assertThat(cached).isNotNull();
        assertThat(cached.price()).isEqualByComparingTo("7.00");
        assertThat(productService.getProductById(id).price()).isEqualByComparingTo("7.00");
    }

    @Test
    void updateWithoutDescriptionClearsItInCache() {
        Long id = createProduct("Plain Blush", "5.00");
        productService.getProductById(id);

        productService.updateProduct(id, new ProductRequest("Plain Blush", new BigDecimal("5.00"), null, null));

        ProductResponse cached = productsCache.get(id, ProductResponse.class);
        assertThat(cached).isNotNull();
        assertThat(cached.description()).isNull();
        assertThat(cached.imageUrl()).isNull();
    }

    @Test
    void deleteEvictsCachedValue() {
        Long id = createProduct("Deleted Blush", "5.00");
        productService.getProductById(id);

        productService.deleteProduct(id);

        assertThat(productsCache.get(id)).isNull();
        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void failedDeleteKeepsCachedValue() {
        Long id = createProduct("Ordered Blush", "5.00");
        orderService.placeOrder(CUSTOMER_ID, new PlaceOrderRequest(List.of(new OrderItemRequest(id, 1))));
        productService.getProductById(id);

        assertThatThrownBy(() -> productService.deleteProduct(id))
                .isInstanceOf(ProductInUseException.class);

        assertThat(productsCache.get(id)).isNotNull();
    }

    @Test
    void cachedEntriesExpireAfterTtl() {
        Long id = createProduct("Expiring Blush", "5.00");
        productService.getProductById(id);

        Long secondsLeft = redisTemplate.getExpire(ProductCacheConfig.PRODUCTS_CACHE + "::" + id);

        assertThat(secondsLeft)
                .isPositive()
                .isLessThanOrEqualTo(ProductCacheConfig.PRODUCT_TTL.toSeconds());
    }

    @Test
    void rolledBackUpdateDoesNotChangeCache() {
        Long id = createProduct("Rolled Back Blush", "5.00");
        productService.getProductById(id);

        transactionTemplate.executeWithoutResult(status -> {
            productService.updateProduct(id, new ProductRequest("Rolled Back Blush", new BigDecimal("9.00"), DESCRIPTION, IMAGE_URL));
            status.setRollbackOnly();
        });

        ProductResponse cached = productsCache.get(id, ProductResponse.class);
        assertThat(cached).isNotNull();
        assertThat(cached.price()).isEqualByComparingTo("5.00");
    }

    private Long createProduct(String name, String price) {
        return productService
                .createProduct(new ProductRequest(name, new BigDecimal(price), DESCRIPTION, IMAGE_URL))
                .id();
    }
}

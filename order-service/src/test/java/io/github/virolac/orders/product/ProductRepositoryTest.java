package io.github.virolac.orders.product;

import io.github.virolac.orders.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void sampleProductsFromMigrationArePresent() {
        assertThat(productRepository.findAll())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Essence Mascara Lash Princess", "Powder Canister", "Red Lipstick");
    }

    @Test
    void saveAssignsId() {
        Product saved = productRepository.save(new Product("Blue Eyeliner", new BigDecimal("7.49")));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void savedProductIsReadBackFromDatabaseWithExactPrice() {
        Product saved = productRepository.save(new Product("Blue Eyeliner", new BigDecimal("7.49")));
        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Blue Eyeliner");
        assertThat(found.getPrice()).isEqualByComparingTo("7.49");
    }

    @Test
    void findByNameReturnsMatchingProduct() {
        List<Product> found = productRepository.findByName("Red Lipstick");

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getPrice()).isEqualByComparingTo("12.99");
    }

    @Test
    void findByNameReturnsEmptyListWhenNothingMatches() {
        assertThat(productRepository.findByName("Does Not Exist")).isEmpty();
    }

    @Test
    void savingProductWithoutNameIsRejected() {
        Product product = new Product(null, new BigDecimal("1.00"));

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingNameLongerThanColumnIsRejected() {
        Product product = new Product("A".repeat(51), new BigDecimal("1.00"));

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingPriceAboveColumnPrecisionIsRejected() {
        Product product = new Product("Too Expensive", new BigDecimal("100000.00"));

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

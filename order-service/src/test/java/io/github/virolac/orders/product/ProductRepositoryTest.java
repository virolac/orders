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
    private static final int SAMPLE_PRODUCT_COUNT = 50;
    private static final String DESCRIPTION = "A soft blue eyeliner.";
    private static final String IMAGE_URL = "https://cdn.example.com/blue-eyeliner.webp";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void sampleCatalogFromMigrationsIsPresent() {
        assertThat(productRepository.findAll()).hasSize(SAMPLE_PRODUCT_COUNT);
    }

    @Test
    void everySampleProductHasDescriptionAndHttpsImage() {
        assertThat(productRepository.findAll()).allSatisfy(product -> {
            assertThat(product.getDescription()).isNotBlank();
            assertThat(product.getImageUrl()).startsWith("https://");
        });
    }

    @Test
    void apostrophesSurviveMigration() {
        List<Product> found = productRepository.findByName("Dior J'adore");

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getDescription()).startsWith("J'adore by Dior");
        assertThat(found.getFirst().getImageUrl()).contains("dior-j'adore");
    }

    @Test
    void nonAsciiCharactersSurviveMigration() {
        Product cookingOil = productRepository.findByName("Cooking Oil").getFirst();

        assertThat(cookingOil.getDescription()).contains("sautéing");
    }

    @Test
    void saveAssignsId() {
        Product saved = productRepository.save(new Product("Blue Eyeliner", new BigDecimal("7.49"), null, null));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void savedProductIsReadBackFromDatabaseWithAllFields() {
        Product saved = productRepository.save(
                new Product("Blue Eyeliner", new BigDecimal("7.49"), DESCRIPTION, IMAGE_URL));
        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Blue Eyeliner");
        assertThat(found.getPrice()).isEqualByComparingTo("7.49");
        assertThat(found.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(found.getImageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void productWithoutDescriptionAndImageIsSavedWithNulls() {
        Product saved = productRepository.save(new Product("Blue Eyeliner", new BigDecimal("7.49"), null, null));
        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getDescription()).isNull();
        assertThat(found.getImageUrl()).isNull();
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
        Product product = new Product(null, new BigDecimal("1.00"), null, null);

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingNameLongerThanColumnIsRejected() {
        Product product = new Product("A".repeat(51), new BigDecimal("1.00"), null, null);

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingPriceAboveColumnPrecisionIsRejected() {
        Product product = new Product("Too Expensive", new BigDecimal("100000.00"), null, null);

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingDescriptionLongerThanColumnIsRejected() {
        Product product = new Product("Wordy Product", new BigDecimal("1.00"), "A".repeat(1001), null);

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

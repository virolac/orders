package io.github.virolac.orders;

import io.github.virolac.orders.product.Product;
import io.github.virolac.orders.product.ProductRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DatabaseRunner implements CommandLineRunner {
    private final ProductRepository productRepository;

    public DatabaseRunner(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String @NonNull ... args) {
        Product essenceMascaraLashPrincess = new Product("Essence Mascara Lash Princess", new BigDecimal("9.99"));
        Product powderCanister = new Product("Powder Canister", new BigDecimal("14.99"));
        Product redLipstick = new Product("Red Lipstick", new BigDecimal("12.99"));

        productRepository.save(essenceMascaraLashPrincess);
        productRepository.save(powderCanister);
        productRepository.save(redLipstick);

        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            System.out.println(p);
        }
    }
}

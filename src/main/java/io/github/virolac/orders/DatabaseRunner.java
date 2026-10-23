package io.github.virolac.orders;

import io.github.virolac.orders.product.Product;
import io.github.virolac.orders.product.ProductRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseRunner implements CommandLineRunner {
    private final ProductRepository productRepository;

    public DatabaseRunner(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String @NonNull ... args) {
        List<Product> products = productRepository.findAll();

        for (Product p : products) {
            System.out.println(p);
        }
    }
}

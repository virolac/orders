package io.github.virolac.orders.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndCustomerId(Long id, String customerId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}

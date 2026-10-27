package io.github.virolac.orders.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record ProductRequest(@NotBlank @Size(max = 50) String name,
                             @NotNull @Digits(integer = 5, fraction = 2) @DecimalMin(value = "0.0") BigDecimal price,
                             @Size(max = 1000) String description,
                             @Size(max = 2048) @URL(protocol = "https") String imageUrl) {
}

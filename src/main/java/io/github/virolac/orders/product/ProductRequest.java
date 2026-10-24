package io.github.virolac.orders.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(@NotBlank @Size(max = 50) String name,
                             @NotNull @Digits(integer = 5, fraction = 2) @DecimalMin(value = "0.0") BigDecimal price) {
}

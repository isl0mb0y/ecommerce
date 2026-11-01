package com.shop.catalog.product;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public ProductSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }
        productRepository.saveAll(List.of(
                new Product("Mechanical Keyboard", 8999, 25),
                new Product("Wireless Mouse", 2999, 50),
                new Product("USB-C Hub", 4599, 30),
                new Product("27-inch Monitor", 24999, 10),
                new Product("Laptop Stand", 3499, 40)));
    }
}

package com.shop.catalog.api.rest;

import java.util.List;

import com.shop.catalog.api.rest.dto.request.CreateProductRequest;
import com.shop.catalog.api.rest.dto.response.ProductResponse;
import com.shop.catalog.domain.Product;
import com.shop.catalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Value("${chaos.delay-ms:0}")
    private long delayMs;

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) throws InterruptedException {
        if (delayMs > 0 ) Thread.sleep(delayMs);
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestBody CreateProductRequest request) {
        if (request.name() == null || request.name().isBlank()
                || request.priceInCents() == null || request.priceInCents() < 0
                || request.stock() == null || request.stock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "name is required; priceInCents and stock must be zero or greater");
        }
        Product product = new Product(request.name(), request.priceInCents(), request.stock());
        return ProductResponse.from(productRepository.save(product));
    }

    @PutMapping("/{id}/price")
    @Transactional
    public ProductResponse updatePrice(@PathVariable Long id, @RequestParam(name = "price_in_cents") long priceInCents) throws InterruptedException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.setPriceInCents(priceInCents);
        return ProductResponse.from(product);
    }
}

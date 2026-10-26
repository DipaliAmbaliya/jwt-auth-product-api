package com.onedao.auth.service;

import com.onedao.auth.dto.PageResponse;
import com.onedao.auth.dto.ProductRequest;
import com.onedao.auth.dto.ProductResponse;
import com.onedao.auth.exception.BadRequestException;
import com.onedao.auth.exception.ResourceNotFoundException;
import com.onedao.auth.model.Product;
import com.onedao.auth.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProductService {

    private static final Set<String> SORTABLE = Set.of("id", "name", "price", "quantity", "createdAt");

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResponse create(ProductRequest req) {
        Product p = new Product();
        apply(p, req);
        return ProductResponse.from(repository.save(p));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String name, BigDecimal minPrice, BigDecimal maxPrice,
                                                int page, int size, String sortBy, String direction) {
        if (page < 0) throw new BadRequestException("page must be >= 0");
        if (size < 1 || size > 100) throw new BadRequestException("size must be between 1 and 100");
        if (!SORTABLE.contains(sortBy)) throw new BadRequestException("sortBy must be one of " + SORTABLE);
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
            throw new BadRequestException("minPrice cannot be greater than maxPrice");

        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        // All values are bound as parameters by JPA Criteria => no SQL injection.
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank())
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.from(repository.findAll(spec, PageRequest.of(page, size, sort))
                .map(ProductResponse::from));
    }

    public ProductResponse update(Long id, ProductRequest req) {
        Product p = find(id);
        apply(p, req);
        return ProductResponse.from(repository.save(p));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Product find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name().trim());
        p.setDescription(req.description() == null ? null : req.description().trim());
        p.setPrice(req.price());
        p.setQuantity(req.quantity());
    }
}

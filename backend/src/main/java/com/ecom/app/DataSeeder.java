package com.ecom.app;

import com.ecom.app.entity.Product;
import com.ecom.app.entity.Role;
import com.ecom.app.entity.User;
import com.ecom.app.repository.ProductRepository;
import com.ecom.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedProducts();
        seedDemoUser();
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }
        productRepository.saveAll(List.of(
                Product.builder().name("Wireless Mouse").description("Ergonomic 2.4GHz wireless mouse with USB receiver.").price(new BigDecimal("19.99")).imageUrl("https://picsum.photos/seed/mouse/400/300").stock(50).build(),
                Product.builder().name("Mechanical Keyboard").description("Tactile mechanical keyboard with RGB backlighting.").price(new BigDecimal("59.99")).imageUrl("https://picsum.photos/seed/keyboard/400/300").stock(30).build(),
                Product.builder().name("27-inch Monitor").description("27-inch QHD IPS monitor, 144Hz refresh rate.").price(new BigDecimal("249.99")).imageUrl("https://picsum.photos/seed/monitor/400/300").stock(15).build(),
                Product.builder().name("USB-C Hub").description("7-in-1 USB-C hub with HDMI, SD card reader, and PD.").price(new BigDecimal("34.99")).imageUrl("https://picsum.photos/seed/hub/400/300").stock(40).build(),
                Product.builder().name("Noise Cancelling Headphones").description("Over-ear headphones with active noise cancellation.").price(new BigDecimal("129.99")).imageUrl("https://picsum.photos/seed/headphones/400/300").stock(25).build(),
                Product.builder().name("Webcam 1080p").description("Full HD webcam with autofocus and built-in mic.").price(new BigDecimal("44.99")).imageUrl("https://picsum.photos/seed/webcam/400/300").stock(35).build(),
                Product.builder().name("Laptop Stand").description("Adjustable aluminum laptop stand, foldable.").price(new BigDecimal("24.99")).imageUrl("https://picsum.photos/seed/stand/400/300").stock(60).build(),
                Product.builder().name("Portable SSD 1TB").description("USB 3.2 portable SSD, up to 1050MB/s read speed.").price(new BigDecimal("89.99")).imageUrl("https://picsum.photos/seed/ssd/400/300").stock(20).build()
        ));
    }

    private void seedDemoUser() {
        if (userRepository.existsByEmail("demo@example.com")) {
            return;
        }
        userRepository.save(User.builder()
                .name("Demo User")
                .email("demo@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.CUSTOMER)
                .build());
    }
}

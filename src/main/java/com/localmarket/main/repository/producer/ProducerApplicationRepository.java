package com.localmarket.main.repository.producer;

import com.localmarket.main.entity.producer.ProducerApplication;
import com.localmarket.main.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.localmarket.main.entity.producer.ApplicationStatus;
import java.util.Optional;

public interface ProducerApplicationRepository extends JpaRepository<ProducerApplication, Long> {
    Optional<ProducerApplication> findByCustomer(User customer);
    Optional<ProducerApplication> findByCustomerAndStatus(User customer, ApplicationStatus status);
    List<ProducerApplication> findByStatus(ApplicationStatus status);
    Optional<ProducerApplication> findTopByCustomerOrderByCreatedAtDesc(User customer);
}
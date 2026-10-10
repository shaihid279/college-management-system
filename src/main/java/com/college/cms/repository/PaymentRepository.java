package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStudentOrderByPaidOnDescIdDesc(Student student);
    boolean existsByReceiptNo(String receiptNo);
}
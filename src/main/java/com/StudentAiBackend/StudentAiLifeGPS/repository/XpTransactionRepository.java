package com.StudentAiBackend.StudentAiLifeGPS.repository;

import com.StudentAiBackend.StudentAiLifeGPS.entity.User;
import com.StudentAiBackend.StudentAiLifeGPS.entity.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {
    List<XpTransaction> findByUserOrderByCreatedAtDesc(User user);
}

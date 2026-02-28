package com.crm.integration_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crm.integration_service.domain.DlqEvent;

public interface DlqEventRepository extends JpaRepository<DlqEvent, String> {}
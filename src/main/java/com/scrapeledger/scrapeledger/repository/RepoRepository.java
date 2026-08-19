package com.scrapeledger.scrapeledger.repository;


import com.scrapeledger.scrapeledger.model.Repo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepoRepository extends JpaRepository<Repo, Long> {
    Optional<Repo> findByName(String name);
}
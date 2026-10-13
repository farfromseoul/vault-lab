package com.example.locker.record;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SecretRecordRepository extends JpaRepository<SecretRecord, UUID> {

	List<SecretRecord> findAllByOrderByCreatedAtDesc();

	List<SecretRecord> findByOwnerOrderByCreatedAtDesc(String owner);

	boolean existsByOwnerAndLabel(String owner, String label);

	/** 재암호화 대상: 최신보다 낮은 키 버전으로 암호화된 레코드. */
	List<SecretRecord> findByKeyVersionLessThan(int keyVersion);

	@Query("select r.keyVersion as keyVersion, count(r) as total from SecretRecord r group by r.keyVersion order by r.keyVersion")
	List<VersionCount> countByKeyVersion();

	interface VersionCount {

		int getKeyVersion();

		long getTotal();

	}

}

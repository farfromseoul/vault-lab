package com.example.locker.record;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 보관 레코드. 평문 컬럼은 없고 암호문만 저장한다.
 */
@Entity
@Table(name = "secret_record",
		uniqueConstraints = @UniqueConstraint(name = "uk_secret_record_owner_label", columnNames = { "owner", "label" }))
public class SecretRecord {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String owner;

	@Column(nullable = false, length = 100)
	private String label;

	@Column(nullable = false, length = 4000)
	private String ciphertext;

	@Column(name = "key_version", nullable = false)
	private int keyVersion;

	@Column(name = "crypto_mode", nullable = false, length = 10)
	private String cryptoMode;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SecretRecord() {
	}

	public SecretRecord(String owner, String label, String ciphertext, int keyVersion, String cryptoMode) {
		this.id = UUID.randomUUID();
		this.owner = owner;
		this.label = label;
		this.ciphertext = ciphertext;
		this.keyVersion = keyVersion;
		this.cryptoMode = cryptoMode;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	/** 재암호화 결과로 암호문을 바꾼다. */
	public void replaceCiphertext(String ciphertext, int keyVersion, String cryptoMode) {
		this.ciphertext = ciphertext;
		this.keyVersion = keyVersion;
		this.cryptoMode = cryptoMode;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return this.id;
	}

	public String getOwner() {
		return this.owner;
	}

	public String getLabel() {
		return this.label;
	}

	public String getCiphertext() {
		return this.ciphertext;
	}

	public int getKeyVersion() {
		return this.keyVersion;
	}

	public String getCryptoMode() {
		return this.cryptoMode;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}

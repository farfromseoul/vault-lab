package com.example.locker.record;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.locker.crypto.Ciphertext;
import com.example.locker.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordService {

	private static final Logger log = LoggerFactory.getLogger(RecordService.class);

	private final SecretRecordRepository repository;

	private final CryptoService crypto;

	public RecordService(SecretRecordRepository repository, CryptoService crypto) {
		this.repository = repository;
		this.crypto = crypto;
	}

	@Transactional
	public SecretRecord create(String owner, String label, String value) {
		if (this.repository.existsByOwnerAndLabel(owner, label)) {
			throw new DuplicateRecordException(owner, label);
		}
		String ciphertext = this.crypto.encrypt(value);
		SecretRecord record = new SecretRecord(owner, label, ciphertext, Ciphertext.versionOf(ciphertext),
				this.crypto.mode());
		return this.repository.save(record);
	}

	@Transactional(readOnly = true)
	public List<SecretRecord> list(String owner) {
		if (owner == null || owner.isBlank()) {
			return this.repository.findAllByOrderByCreatedAtDesc();
		}
		return this.repository.findByOwnerOrderByCreatedAtDesc(owner);
	}

	@Transactional(readOnly = true)
	public SecretRecord get(UUID id) {
		return this.repository.findById(id).orElseThrow(() -> new RecordNotFoundException(id));
	}

	/** 복호화한 값을 돌려준다. 조회 사실을 로그에 남기되 값은 남기지 않는다. */
	@Transactional(readOnly = true)
	public Decrypted read(UUID id) {
		SecretRecord record = get(id);
		String value = this.crypto.decrypt(record.getCiphertext());
		log.info("record.read id={} owner={}", record.getId(), record.getOwner());
		return new Decrypted(record, value);
	}

	@Transactional
	public void delete(UUID id) {
		SecretRecord record = get(id);
		this.repository.delete(record);
		log.info("record.delete id={} owner={}", record.getId(), record.getOwner());
	}

	/** 최신보다 낮은 키 버전의 레코드를 최신 버전으로 다시 감싼다. 처리한 건수를 돌려준다. */
	@Transactional
	public int rewrapOutdated() {
		int latest = this.crypto.latestKeyVersion();
		List<SecretRecord> outdated = this.repository.findByKeyVersionLessThan(latest);
		for (SecretRecord record : outdated) {
			String rewrapped = this.crypto.rewrap(record.getCiphertext());
			record.replaceCiphertext(rewrapped, Ciphertext.versionOf(rewrapped), this.crypto.mode());
		}
		log.info("record.rewrap count={} latestVersion={}", outdated.size(), latest);
		return outdated.size();
	}

	/** 키 버전별 레코드 수. 키는 "v1", "v2" 형태. */
	@Transactional(readOnly = true)
	public Map<String, Long> countByKeyVersion() {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (SecretRecordRepository.VersionCount row : this.repository.countByKeyVersion()) {
			counts.put("v" + row.getKeyVersion(), row.getTotal());
		}
		return counts;
	}

	public record Decrypted(SecretRecord record, String value) {
	}

}

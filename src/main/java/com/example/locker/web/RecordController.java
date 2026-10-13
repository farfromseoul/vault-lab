package com.example.locker.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.locker.record.RecordService;
import com.example.locker.record.SecretRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class RecordController {

	private final RecordService service;

	public RecordController(RecordService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RecordSummary create(@Valid @RequestBody CreateRequest request) {
		return RecordSummary.of(this.service.create(request.owner().trim(), request.label().trim(), request.value()));
	}

	/** 평문 없이 메타데이터와 암호문만 돌려준다. */
	@GetMapping
	public List<RecordSummary> list(@RequestParam(required = false) String owner) {
		return this.service.list(owner).stream().map(RecordSummary::of).toList();
	}

	/** 한 건을 복호화해 돌려준다. */
	@GetMapping("/{id}")
	public RecordDetail read(@PathVariable UUID id) {
		RecordService.Decrypted decrypted = this.service.read(id);
		return RecordDetail.of(decrypted.record(), decrypted.value());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		this.service.delete(id);
	}

	@PostMapping("/rewrap")
	public RewrapResult rewrap() {
		return new RewrapResult(this.service.rewrapOutdated());
	}

	public record CreateRequest(
			@NotBlank(message = "owner는 필수입니다") @Size(max = 100, message = "owner는 100자 이하여야 합니다") String owner,
			@NotBlank(message = "label은 필수입니다") @Size(max = 100, message = "label은 100자 이하여야 합니다") String label,
			@NotBlank(message = "value는 필수입니다") @Size(max = 1000, message = "value는 1000자 이하여야 합니다") String value) {

		// 요청 객체가 로그에 찍혀도 값이 드러나지 않게 한다
		@Override
		public String toString() {
			return "CreateRequest[owner=" + owner + ", label=" + label + "]";
		}
	}

	public record RecordSummary(UUID id, String owner, String label, int keyVersion, String cryptoMode,
			String ciphertext, Instant createdAt, Instant updatedAt) {

		static RecordSummary of(SecretRecord record) {
			return new RecordSummary(record.getId(), record.getOwner(), record.getLabel(), record.getKeyVersion(),
					record.getCryptoMode(), record.getCiphertext(), record.getCreatedAt(), record.getUpdatedAt());
		}
	}

	public record RecordDetail(UUID id, String owner, String label, int keyVersion, String cryptoMode, String value,
			Instant createdAt, Instant updatedAt) {

		static RecordDetail of(SecretRecord record, String value) {
			return new RecordDetail(record.getId(), record.getOwner(), record.getLabel(), record.getKeyVersion(),
					record.getCryptoMode(), value, record.getCreatedAt(), record.getUpdatedAt());
		}

		@Override
		public String toString() {
			return "RecordDetail[id=" + id + ", owner=" + owner + ", label=" + label + "]";
		}
	}

	public record RewrapResult(int rewrapped) {
	}

}

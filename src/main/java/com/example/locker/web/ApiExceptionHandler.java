package com.example.locker.web;

import java.util.stream.Collectors;

import com.example.locker.crypto.CryptoException;
import com.example.locker.record.DuplicateRecordException;
import com.example.locker.record.RecordNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.vault.VaultException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * API 오류 응답. 본문은 {"error": 코드, "message": 설명}이고 평문, 암호문, 토큰을 담지 않는다.
 */
@RestControllerAdvice(basePackages = "com.example.locker.web")
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map((error) -> error.getDefaultMessage())
			.collect(Collectors.joining(", "));
		return respond(HttpStatus.BAD_REQUEST, "invalid_request", message);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ApiError> unreadable(Exception ex) {
		return respond(HttpStatus.BAD_REQUEST, "invalid_request", "요청 형식이 올바르지 않습니다");
	}

	@ExceptionHandler(RecordNotFoundException.class)
	public ResponseEntity<ApiError> notFound(RecordNotFoundException ex) {
		return respond(HttpStatus.NOT_FOUND, "not_found", ex.getMessage());
	}

	@ExceptionHandler(DuplicateRecordException.class)
	public ResponseEntity<ApiError> duplicate(DuplicateRecordException ex) {
		return respond(HttpStatus.CONFLICT, "duplicate", ex.getMessage());
	}

	// 중복 확인과 저장 사이에 다른 요청이 끼어든 경우
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> constraint(DataIntegrityViolationException ex) {
		return respond(HttpStatus.CONFLICT, "duplicate", "같은 소유자와 라벨의 레코드가 이미 있습니다");
	}

	// Vault 권한 거부, 토큰 만료, 연결 실패. Vault의 응답 본문은 클라이언트에 전달하지 않는다.
	@ExceptionHandler({ VaultException.class, RestClientException.class })
	public ResponseEntity<ApiError> vaultUnavailable(RuntimeException ex) {
		log.warn("vault.call failed cause={}", ex.getClass().getSimpleName());
		return respond(HttpStatus.SERVICE_UNAVAILABLE, "vault_unavailable",
				"Vault 호출에 실패했습니다. 토큰이 만료됐다면 다시 배포해야 합니다");
	}

	@ExceptionHandler(CryptoException.class)
	public ResponseEntity<ApiError> crypto(CryptoException ex) {
		log.warn("crypto failed message={}", ex.getMessage());
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "crypto_failed", "암복호화에 실패했습니다");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> unexpected(Exception ex) {
		log.error("unexpected error type={}", ex.getClass().getName());
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "처리 중 오류가 발생했습니다");
	}

	private ResponseEntity<ApiError> respond(HttpStatus status, String error, String message) {
		return ResponseEntity.status(status).body(new ApiError(error, message));
	}

	public record ApiError(String error, String message) {
	}

}

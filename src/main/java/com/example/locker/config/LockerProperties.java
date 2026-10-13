package com.example.locker.config;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml의 locker.* 설정. 필수 값이 비어 있으면 어떤 값이 없는지 알리고 기동에 실패한다.
 */
@ConfigurationProperties("locker")
@Validated
public record LockerProperties(@Valid @NotNull Vault vault, @Valid @NotNull Crypto crypto, @Valid @NotNull Db db) {

	public record Vault(
			@NotBlank(message = "VAULT_ADDR가 비어 있습니다") String uri,
			@NotBlank(message = "VAULT_ROLE_ID가 비어 있습니다") String roleId,
			@NotBlank(message = "VAULT_WRAPPED_SECRET_ID가 비어 있습니다") String wrappedSecretId,
			@NotBlank String keyName) {

		/** 로그용. role-id는 앞 8자만 남긴다. */
		public String maskedRoleId() {
			return roleId.length() <= 8 ? "********" : roleId.substring(0, 8) + "…";
		}

		// 래핑 토큰과 role-id가 로그에 찍히지 않게 한다
		@Override
		public String toString() {
			return "Vault[uri=" + uri + ", roleId=" + maskedRoleId() + ", keyName=" + keyName + "]";
		}
	}

	public record Crypto(
			@NotBlank @Pattern(regexp = "transit|local", message = "LOCKER_CRYPTO_MODE는 transit 또는 local이어야 합니다") String mode,
			@NotNull Duration keyRefresh) {
	}

	public record Db(@NotBlank String path) {
	}

}

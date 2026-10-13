package com.example.locker.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.RoleId;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.SecretId;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.VaultToken;

/**
 * Vault 연결과 AppRole 인증 구성.
 *
 * <p>
 * Jenkins가 넘겨준 래핑 토큰으로 secret-id를 꺼내(unwrap) role-id와 함께 로그인한다. 로그인으로 받은 토큰의 갱신은
 * 상위 클래스가 등록하는 세션 관리자가 맡는다.
 */
@Configuration
public class VaultConfig extends AbstractVaultConfiguration {

	private final LockerProperties props;

	public VaultConfig(LockerProperties props) {
		this.props = props;
	}

	@Override
	public VaultEndpoint vaultEndpoint() {
		return VaultEndpoint.from(URI.create(this.props.vault().uri()));
	}

	@Override
	public ClientAuthentication clientAuthentication() {
		AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
			.roleId(RoleId.provided(this.props.vault().roleId()))
			// 래핑 토큰은 한 번만 풀 수 있다. 재로그인이 필요해지면 다시 배포해야 한다.
			.secretId(SecretId.wrapped(VaultToken.of(this.props.vault().wrappedSecretId())))
			.build();
		return new AppRoleAuthentication(options, vaultClient());
	}

	@Bean
	public VaultTransitOperations vaultTransitOperations(VaultTemplate vaultTemplate) {
		return vaultTemplate.opsForTransit();
	}

}

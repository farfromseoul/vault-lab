package com.example.locker.config;

import com.example.locker.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 기동 직후 Vault를 한 번 호출해 로그인과 권한을 확인한다. 실패하면 앱을 띄우지 않는다.
 */
@Component
public class StartupCheck implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(StartupCheck.class);

	private final CryptoService crypto;

	private final LockerProperties props;

	public StartupCheck(CryptoService crypto, LockerProperties props) {
		this.crypto = crypto;
		this.props = props;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			int latest = this.crypto.latestKeyVersion();
			log.info("vault.login ok mode={} key={} latestVersion={} roleId={}", this.crypto.mode(),
					this.props.vault().keyName(), latest, this.props.vault().maskedRoleId());
		}
		catch (RuntimeException ex) {
			// 원인 메시지에 토큰이 섞이지 않도록 예외 종류만 남긴다
			log.error("vault.login failed uri={} key={} cause={}", this.props.vault().uri(),
					this.props.vault().keyName(), ex.getClass().getSimpleName());
			throw new IllegalStateException(
					"Vault 로그인 또는 키 조회에 실패했습니다. 래핑 토큰이 만료됐거나 이미 사용됐는지, 정책에 권한이 있는지 확인하세요.", ex);
		}
	}

}

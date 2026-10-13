package com.example.locker.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.VaultTransitKey;

/**
 * /actuator/health의 vault 항목. 앱 토큰으로 Transit 키 메타데이터를 읽을 수 있는지 본다. 토큰이 만료되면 DOWN이
 * 된다.
 */
@Component("vault")
public class VaultHealthIndicator implements HealthIndicator {

	private final VaultTransitOperations transit;

	private final LockerProperties props;

	public VaultHealthIndicator(VaultTransitOperations transit, LockerProperties props) {
		this.transit = transit;
		this.props = props;
	}

	@Override
	public Health health() {
		String keyName = this.props.vault().keyName();
		try {
			VaultTransitKey key = this.transit.getKey(keyName);
			if (key == null) {
				return Health.down().withDetail("key", keyName).withDetail("error", "KeyNotFound").build();
			}
			return Health.up().withDetail("key", keyName).withDetail("latestVersion", key.getLatestVersion()).build();
		}
		catch (RuntimeException ex) {
			return Health.down().withDetail("key", keyName).withDetail("error", ex.getClass().getSimpleName()).build();
		}
	}

}

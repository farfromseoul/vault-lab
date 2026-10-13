package com.example.locker.crypto;

import com.example.locker.config.LockerProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.VaultTransitKey;

/**
 * 평문을 Vault에 보내 암호문을 받는다. 키는 Vault 밖으로 나오지 않고, 호출마다 감사 로그가 남는다.
 */
@Service
@ConditionalOnProperty(prefix = "locker.crypto", name = "mode", havingValue = "transit", matchIfMissing = true)
public class TransitCryptoService implements CryptoService {

	private final VaultTransitOperations transit;

	private final String keyName;

	public TransitCryptoService(VaultTransitOperations transit, LockerProperties props) {
		this.transit = transit;
		this.keyName = props.vault().keyName();
	}

	@Override
	public String encrypt(String plaintext) {
		return this.transit.encrypt(this.keyName, plaintext);
	}

	@Override
	public String decrypt(String ciphertext) {
		return this.transit.decrypt(this.keyName, ciphertext);
	}

	@Override
	public String rewrap(String ciphertext) {
		return this.transit.rewrap(this.keyName, ciphertext);
	}

	@Override
	public int latestKeyVersion() {
		VaultTransitKey key = this.transit.getKey(this.keyName);
		if (key == null) {
			throw new CryptoException("Transit 키를 찾을 수 없습니다: " + this.keyName);
		}
		return key.getLatestVersion();
	}

	@Override
	public String mode() {
		return "transit";
	}

}

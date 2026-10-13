package com.example.locker.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * export한 키로 앱 안에서 AES-256-GCM 암복호화를 한다. Vault를 호출하지 않는다.
 *
 * <p>
 * 출력은 Vault Transit의 aes256-gcm96 암호문과 같은 형식이다:
 * {@code vault:v<버전>:base64(nonce 12바이트 + 암호문 + 태그 16바이트)}. 그래서 여기서 만든 암호문을 Vault의
 * transit/decrypt로 풀 수 있어야 한다.
 */
@Service
@ConditionalOnProperty(prefix = "locker.crypto", name = "mode", havingValue = "local")
public class LocalCryptoService implements CryptoService {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private static final int NONCE_BYTES = 12;

	private static final int TAG_BITS = 128;

	private final KeySource keys;

	private final SecureRandom random = new SecureRandom();

	public LocalCryptoService(KeySource keys) {
		this.keys = keys;
	}

	@Override
	public String encrypt(String plaintext) {
		int version = this.keys.latestVersion();
		SecretKey key = this.keys.key(version).orElseThrow(() -> new CryptoException("최신 버전의 키가 없습니다"));
		byte[] nonce = new byte[NONCE_BYTES];
		this.random.nextBytes(nonce);
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
			byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			byte[] payload = new byte[NONCE_BYTES + sealed.length];
			System.arraycopy(nonce, 0, payload, 0, NONCE_BYTES);
			System.arraycopy(sealed, 0, payload, NONCE_BYTES, sealed.length);
			return Ciphertext.format(version, payload);
		}
		catch (GeneralSecurityException ex) {
			throw new CryptoException("암호화에 실패했습니다", ex);
		}
	}

	@Override
	public String decrypt(String ciphertext) {
		Ciphertext parsed = Ciphertext.parse(ciphertext);
		byte[] payload = parsed.payload();
		if (payload.length < NONCE_BYTES + TAG_BITS / 8) {
			throw new CryptoException("암호문이 너무 짧습니다");
		}
		SecretKey key = keyFor(parsed.version());
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key,
					new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(payload, 0, NONCE_BYTES)));
			byte[] plain = cipher.doFinal(payload, NONCE_BYTES, payload.length - NONCE_BYTES);
			return new String(plain, StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException ex) {
			// 인증 태그 불일치(변조 또는 다른 키) 포함
			throw new CryptoException("복호화에 실패했습니다. 암호문이 손상됐거나 키가 다릅니다", ex);
		}
	}

	@Override
	public String rewrap(String ciphertext) {
		return encrypt(decrypt(ciphertext));
	}

	@Override
	public int latestKeyVersion() {
		return this.keys.latestVersion();
	}

	@Override
	public String mode() {
		return "local";
	}

	private SecretKey keyFor(int version) {
		return this.keys.key(version).orElseGet(() -> {
			// 모르는 버전이면 rotate된 것일 수 있으니 한 번 다시 가져와 본다
			this.keys.refresh();
			return this.keys.key(version)
				.orElseThrow(() -> new CryptoException("키 버전 v" + version + "이 없습니다"));
		});
	}

}

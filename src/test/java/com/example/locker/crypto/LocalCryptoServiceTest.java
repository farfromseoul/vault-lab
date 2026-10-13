package com.example.locker.crypto;

import java.util.Base64;
import java.util.Optional;
import java.util.TreeMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalCryptoServiceTest {

	@Test
	void encryptThenDecryptReturnsPlaintext() {
		LocalCryptoService crypto = new LocalCryptoService(new FixedKeys().with(1));

		String ciphertext = crypto.encrypt("110-123-456789");

		assertThat(ciphertext).startsWith("vault:v1:").doesNotContain("110-123-456789");
		assertThat(crypto.decrypt(ciphertext)).isEqualTo("110-123-456789");
	}

	@Test
	void payloadHasVaultLayout() {
		LocalCryptoService crypto = new LocalCryptoService(new FixedKeys().with(1));

		byte[] payload = Ciphertext.parse(crypto.encrypt("abc")).payload();

		// nonce 12바이트 + 평문 3바이트 + 태그 16바이트
		assertThat(payload).hasSize(12 + 3 + 16);
	}

	@Test
	void samePlaintextEncryptsDifferentlyEachTime() {
		LocalCryptoService crypto = new LocalCryptoService(new FixedKeys().with(1));

		assertThat(crypto.encrypt("same")).isNotEqualTo(crypto.encrypt("same"));
	}

	@Test
	void tamperedCiphertextIsRejected() {
		LocalCryptoService crypto = new LocalCryptoService(new FixedKeys().with(1));
		byte[] payload = Ciphertext.parse(crypto.encrypt("secret")).payload();
		payload[payload.length - 1] ^= 0x01;

		String tampered = Ciphertext.format(1, payload);

		assertThatThrownBy(() -> crypto.decrypt(tampered)).isInstanceOf(CryptoException.class);
	}

	@Test
	void tooShortCiphertextIsRejected() {
		LocalCryptoService crypto = new LocalCryptoService(new FixedKeys().with(1));

		String tooShort = "vault:v1:" + Base64.getEncoder().encodeToString(new byte[10]);

		assertThatThrownBy(() -> crypto.decrypt(tooShort)).isInstanceOf(CryptoException.class);
	}

	@Test
	void encryptsWithLatestVersionAndDecryptsOlderOnes() {
		FixedKeys keys = new FixedKeys().with(1);
		LocalCryptoService crypto = new LocalCryptoService(keys);
		String v1 = crypto.encrypt("old");

		keys.with(2);
		String v2 = crypto.encrypt("new");

		assertThat(v2).startsWith("vault:v2:");
		assertThat(crypto.latestKeyVersion()).isEqualTo(2);
		assertThat(crypto.decrypt(v1)).isEqualTo("old");
		assertThat(crypto.decrypt(v2)).isEqualTo("new");
	}

	@Test
	void rewrapMovesCiphertextToLatestVersion() {
		FixedKeys keys = new FixedKeys().with(1);
		LocalCryptoService crypto = new LocalCryptoService(keys);
		String v1 = crypto.encrypt("value");
		keys.with(2);

		String rewrapped = crypto.rewrap(v1);

		assertThat(rewrapped).startsWith("vault:v2:");
		assertThat(crypto.decrypt(rewrapped)).isEqualTo("value");
	}

	@Test
	void unknownVersionTriggersOneRefresh() {
		FixedKeys keys = new FixedKeys().with(1);
		LocalCryptoService crypto = new LocalCryptoService(keys);
		// v2 키는 Vault에만 있고 아직 가져오지 않은 상황
		keys.availableAfterRefresh(2);
		String v2 = new LocalCryptoService(new FixedKeys().with(1).with(2)).encrypt("rotated");

		assertThat(crypto.decrypt(v2)).isEqualTo("rotated");
		assertThat(keys.refreshCount).isEqualTo(1);
	}

	@Test
	void missingVersionFailsAfterRefresh() {
		FixedKeys keys = new FixedKeys().with(1);
		LocalCryptoService crypto = new LocalCryptoService(keys);
		String v3 = new LocalCryptoService(new FixedKeys().with(3)).encrypt("x");

		assertThatThrownBy(() -> crypto.decrypt(v3)).isInstanceOf(CryptoException.class);
		assertThat(keys.refreshCount).isEqualTo(1);
	}

	/** 버전 번호로 결정되는 고정 키. 같은 버전은 항상 같은 키다. */
	private static final class FixedKeys implements KeySource {

		private final TreeMap<Integer, SecretKey> keys = new TreeMap<>();

		private final TreeMap<Integer, SecretKey> pending = new TreeMap<>();

		private int refreshCount;

		FixedKeys with(int version) {
			this.keys.put(version, keyFor(version));
			return this;
		}

		void availableAfterRefresh(int version) {
			this.pending.put(version, keyFor(version));
		}

		private static SecretKey keyFor(int version) {
			byte[] material = new byte[32];
			java.util.Arrays.fill(material, (byte) version);
			return new SecretKeySpec(material, "AES");
		}

		@Override
		public Optional<SecretKey> key(int version) {
			return Optional.ofNullable(this.keys.get(version));
		}

		@Override
		public int latestVersion() {
			return this.keys.lastKey();
		}

		@Override
		public void refresh() {
			this.refreshCount++;
			this.keys.putAll(this.pending);
			this.pending.clear();
		}

	}

}

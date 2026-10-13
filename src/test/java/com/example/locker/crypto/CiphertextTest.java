package com.example.locker.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CiphertextTest {

	@Test
	void formatAndParseRoundTrip() {
		byte[] payload = { 1, 2, 3, 4, 5 };

		String text = Ciphertext.format(3, payload);
		Ciphertext parsed = Ciphertext.parse(text);

		assertThat(text).startsWith("vault:v3:");
		assertThat(parsed.version()).isEqualTo(3);
		assertThat(parsed.payload()).containsExactly(payload);
	}

	@Test
	void versionOfReadsPrefix() {
		assertThat(Ciphertext.versionOf("vault:v12:AAAA")).isEqualTo(12);
	}

	@Test
	void rejectsMalformedText() {
		assertThatThrownBy(() -> Ciphertext.parse(null)).isInstanceOf(CryptoException.class);
		assertThatThrownBy(() -> Ciphertext.parse("plain text")).isInstanceOf(CryptoException.class);
		assertThatThrownBy(() -> Ciphertext.parse("vault:v1")).isInstanceOf(CryptoException.class);
		assertThatThrownBy(() -> Ciphertext.parse("vault:vX:AAAA")).isInstanceOf(CryptoException.class);
		assertThatThrownBy(() -> Ciphertext.parse("vault:v0:AAAA")).isInstanceOf(CryptoException.class);
		assertThatThrownBy(() -> Ciphertext.parse("vault:v1:!!!not-base64!!!")).isInstanceOf(CryptoException.class);
	}

}

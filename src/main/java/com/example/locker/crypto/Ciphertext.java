package com.example.locker.crypto;

import java.util.Base64;

/**
 * Vault Transit 암호문 형식 {@code vault:v<버전>:<base64>}의 파싱과 조립.
 */
public final class Ciphertext {

	private static final String PREFIX = "vault:v";

	private final int version;

	private final byte[] payload;

	private Ciphertext(int version, byte[] payload) {
		this.version = version;
		this.payload = payload;
	}

	public static Ciphertext parse(String text) {
		if (text == null || !text.startsWith(PREFIX)) {
			throw new CryptoException("암호문 형식이 올바르지 않습니다");
		}
		int sep = text.indexOf(':', PREFIX.length());
		if (sep < 0) {
			throw new CryptoException("암호문 형식이 올바르지 않습니다");
		}
		try {
			int version = Integer.parseInt(text.substring(PREFIX.length(), sep));
			if (version < 1) {
				throw new CryptoException("암호문의 키 버전이 올바르지 않습니다");
			}
			return new Ciphertext(version, Base64.getDecoder().decode(text.substring(sep + 1)));
		}
		catch (IllegalArgumentException ex) {
			// NumberFormatException 포함. 원문은 메시지에 싣지 않는다.
			throw new CryptoException("암호문 형식이 올바르지 않습니다");
		}
	}

	public static String format(int version, byte[] payload) {
		return PREFIX + version + ":" + Base64.getEncoder().encodeToString(payload);
	}

	public static int versionOf(String text) {
		return parse(text).version();
	}

	public int version() {
		return this.version;
	}

	public byte[] payload() {
		return this.payload.clone();
	}

}

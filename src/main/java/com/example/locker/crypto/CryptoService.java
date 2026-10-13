package com.example.locker.crypto;

/**
 * 암복호화 계층. 구현체는 locker.crypto.mode 값에 따라 하나만 등록된다. 두 구현체 모두 암호문을
 * {@code vault:v<버전>:<base64>} 형식으로 다룬다.
 */
public interface CryptoService {

	String encrypt(String plaintext);

	String decrypt(String ciphertext);

	/** 최신 키 버전으로 다시 감싼다. */
	String rewrap(String ciphertext);

	int latestKeyVersion();

	/** "transit" 또는 "local". */
	String mode();

}

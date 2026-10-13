package com.example.locker.crypto;

/**
 * 암호문이 손상됐거나 복호화할 키가 없을 때. 메시지에 평문, 암호문, 키를 담지 않는다.
 */
public class CryptoException extends RuntimeException {

	public CryptoException(String message) {
		super(message);
	}

	public CryptoException(String message, Throwable cause) {
		super(message, cause);
	}

}

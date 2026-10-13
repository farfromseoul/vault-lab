package com.example.locker.crypto;

import java.util.Optional;

import javax.crypto.SecretKey;

/**
 * 버전별 암호화 키를 내주는 곳. 운영 구현은 {@link ExportedKeyStore}이고, 테스트에서는 고정 키로 대체한다.
 */
public interface KeySource {

	Optional<SecretKey> key(int version);

	int latestVersion();

	/** 키를 다시 가져온다. 키가 rotate됐을 때 새 버전을 반영한다. */
	void refresh();

}

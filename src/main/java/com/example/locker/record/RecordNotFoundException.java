package com.example.locker.record;

import java.util.UUID;

public class RecordNotFoundException extends RuntimeException {

	public RecordNotFoundException(UUID id) {
		super("레코드를 찾을 수 없습니다: " + id);
	}

}

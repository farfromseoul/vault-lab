package com.example.locker.record;

public class DuplicateRecordException extends RuntimeException {

	public DuplicateRecordException(String owner, String label) {
		super("같은 소유자와 라벨의 레코드가 이미 있습니다: " + owner + " / " + label);
	}

}

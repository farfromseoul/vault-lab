package com.example.locker.web;

import java.util.Map;

import com.example.locker.config.LockerProperties;
import com.example.locker.crypto.CryptoService;
import com.example.locker.record.RecordService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

	private final CryptoService crypto;

	private final RecordService records;

	private final LockerProperties props;

	public CryptoController(CryptoService crypto, RecordService records, LockerProperties props) {
		this.crypto = crypto;
		this.records = records;
		this.props = props;
	}

	/** 현재 암호화 방식과 키 상태. 키 원본은 담지 않는다. */
	@GetMapping("/status")
	public Status status() {
		return new Status(this.crypto.mode(), this.props.vault().keyName(), this.crypto.latestKeyVersion(),
				this.records.countByKeyVersion());
	}

	public record Status(String mode, String keyName, int latestKeyVersion, Map<String, Long> recordsByKeyVersion) {
	}

}

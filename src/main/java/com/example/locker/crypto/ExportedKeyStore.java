package com.example.locker.crypto;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.example.locker.config.LockerProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.RawTransitKey;
import org.springframework.vault.support.TransitKeyType;

/**
 * local 방식에서 쓰는 키 보관소. Vault에서 키 원본을 export해 메모리에만 둔다. 디스크와 로그에는 쓰지 않는다.
 *
 * <p>
 * 한 번 export한 키는 앱 토큰이 만료되거나 폐기돼도 메모리에 남는다. 이것이 키 export 방식의 위험이다.
 */
@Component
@ConditionalOnProperty(prefix = "locker.crypto", name = "mode", havingValue = "local")
public class ExportedKeyStore implements KeySource {

	private static final Logger log = LoggerFactory.getLogger(ExportedKeyStore.class);

	private final VaultTransitOperations transit;

	private final String keyName;

	private final Duration refreshInterval;

	private volatile NavigableMap<Integer, SecretKey> keys = new TreeMap<>();

	private ScheduledExecutorService scheduler;

	public ExportedKeyStore(VaultTransitOperations transit, LockerProperties props) {
		this.transit = transit;
		this.keyName = props.vault().keyName();
		this.refreshInterval = props.crypto().keyRefresh();
	}

	@PostConstruct
	void start() {
		// 첫 export가 실패하면 앱을 띄우지 않는다
		refresh();
		this.scheduler = Executors.newSingleThreadScheduledExecutor((runnable) -> {
			Thread thread = new Thread(runnable, "key-refresh");
			thread.setDaemon(true);
			return thread;
		});
		long millis = this.refreshInterval.toMillis();
		this.scheduler.scheduleWithFixedDelay(this::refreshQuietly, millis, millis, TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	void stop() {
		if (this.scheduler != null) {
			this.scheduler.shutdownNow();
		}
	}

	@Override
	public Optional<SecretKey> key(int version) {
		return Optional.ofNullable(this.keys.get(version));
	}

	@Override
	public int latestVersion() {
		NavigableMap<Integer, SecretKey> current = this.keys;
		if (current.isEmpty()) {
			throw new CryptoException("export한 키가 없습니다");
		}
		return current.lastKey();
	}

	@Override
	public synchronized void refresh() {
		RawTransitKey raw = this.transit.exportKey(this.keyName, TransitKeyType.ENCRYPTION_KEY);
		if (raw == null || raw.getKeys() == null || raw.getKeys().isEmpty()) {
			throw new CryptoException("Vault가 키를 돌려주지 않았습니다. 키가 exportable인지 확인하세요");
		}
		NavigableMap<Integer, SecretKey> loaded = new TreeMap<>();
		for (Map.Entry<String, String> entry : raw.getKeys().entrySet()) {
			byte[] material = Base64.getDecoder().decode(entry.getValue());
			loaded.put(Integer.parseInt(entry.getKey()), new SecretKeySpec(material, "AES"));
		}
		boolean changed = !loaded.keySet().equals(this.keys.keySet());
		this.keys = loaded;
		if (changed) {
			log.info("key.export ok key={} versions={}", this.keyName, loaded.keySet());
		}
	}

	private void refreshQuietly() {
		try {
			refresh();
		}
		catch (RuntimeException ex) {
			// 갱신에 실패해도 메모리의 키로 계속 동작한다
			log.warn("key.export failed key={} cause={}", this.keyName, ex.getClass().getSimpleName());
		}
	}

}

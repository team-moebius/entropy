package com.moebius.entropy.repository;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class InflationLockRepository {

	private final Set<String> inflationLocks = new HashSet<>();

	public boolean getAndSetLock(String id) {
		synchronized (this) {
			if (!inflationLocks.contains(id)) {
				inflationLocks.add(id);
				return true;
			}
			return false;
		}
	}

	public void unsetLock(String id) {
		inflationLocks.remove(id);
	}
}

package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;

public class MockStatusChecker implements StatusChecker {

	private final String name;

	public MockStatusChecker(String name) {
		this.name = name;
	}

	@NotNull
	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
	}
}

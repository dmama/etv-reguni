package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.webservice.sipf.BVRPlusClient;

public class ServiceBVRChecker implements StatusChecker {

	private BVRPlusClient bvrClient;

	@NotNull
	@Override
	public String getName() {
		return "serviceBVRPlus";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			bvrClient.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBvrClient(BVRPlusClient bvrClient) {
		this.bvrClient = bvrClient;
	}
}
package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClient;

public class ServiceBVRChecker implements ServiceChecker {

	private BVRPlusClient bvrClient;
	private String details;

	@Override
	public Status getStatus() {
		try {
			bvrClient.ping();
			return Status.OK;
		}
		catch (Exception e) {
			details = ExceptionUtils.extractCallStack(e);
			return Status.KO;
		}
	}

	@Override
	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBvrClient(BVRPlusClient bvrClient) {
		this.bvrClient = bvrClient;
	}
}
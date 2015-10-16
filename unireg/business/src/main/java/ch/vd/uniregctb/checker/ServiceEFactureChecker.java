package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.efacture.EFactureService;

public class ServiceEFactureChecker implements ServiceChecker {

	private EFactureClient efactureClient;
	private String details;

	@Override
	public Status getStatus() {
		try {
			efactureClient.getHistory(10000000L, EFactureService.ACI_BILLER_ID);
			details = null;
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

	public void setEfactureClient(EFactureClient efactureClient) {
		this.efactureClient = efactureClient;
	}
}

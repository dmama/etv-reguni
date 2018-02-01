package ch.vd.uniregctb.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.efacture.EFactureService;

public class ServiceEFactureChecker implements StatusChecker {

	private EFactureClient efactureClient;

	@NotNull
	@Override
	public String getName() {
		return "serviceEFacture";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			efactureClient.getHistory(10000000L, EFactureService.ACI_BILLER_ID);
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	public void setEfactureClient(EFactureClient efactureClient) {
		this.efactureClient = efactureClient;
	}
}

package ch.vd.uniregctb.checker;

import java.math.BigInteger;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClient;

public class ServiceBVRChecker implements ServiceChecker {

	private BVRPlusClient bvrClient;
	private String details;

	public Status getStatus() {
		try {
			final BvrDemande demande = new BvrDemande();
			demande.setNdc("0");
			demande.setAnneeTaxation(BigInteger.valueOf(2009));
			demande.setTypeDebiteurIS("REGULIER");

			bvrClient.ping();
			return Status.OK;
		}
		catch (Exception e) {
			details = ExceptionUtils.extractCallStack(e);
			return Status.KO;
		}
	}

	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBvrClient(BVRPlusClient bvrClient) {
		this.bvrClient = bvrClient;
	}
}
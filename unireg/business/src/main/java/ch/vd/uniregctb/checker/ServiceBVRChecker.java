package ch.vd.uniregctb.checker;

import java.math.BigInteger;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClient;

public class ServiceBVRChecker {

	private BVRPlusClient bvrClient;
	private String details;

	public Status getStatus() {
		try {
			final BvrDemande demande = new BvrDemande();
			demande.setNdc("0");
			demande.setAnneeTaxation(BigInteger.valueOf(2009));
			demande.setTypeDebiteurIS("REGULIER");

			final BvrReponse reponse = bvrClient.getBVRDemande(demande);
			// on essaie avec le débiteur 0 qui n'existe pas pour ne pas générer de nouveau numéro de BVR, la seule chose qui nous intéresse, c'est de recevoir une réponse
			Assert.isTrue(reponse.getMessage().contains("CONTRIB_ABSENT"), "La réponse est vide ou incorrecte.");
			details = null;
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
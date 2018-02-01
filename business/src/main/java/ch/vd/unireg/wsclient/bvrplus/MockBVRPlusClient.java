package ch.vd.unireg.wsclient.bvrplus;

import java.util.Random;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.unireg.webservice.sipf.BVRPlusClient;
import ch.vd.unireg.webservice.sipf.BVRPlusClientException;

public class MockBVRPlusClient implements BVRPlusClient {

	private static final Random RND = new Random();

	@Override
	public BvrReponse getBVRDemande(BvrDemande bvrDemande) throws BVRPlusClientException {
		final BvrReponse response = new BvrReponse();
		final String noRef = buildNoRef(bvrDemande);
		response.setAnneeTaxation(bvrDemande.getAnneeTaxation());
		response.setNdc(bvrDemande.getNdc());
		response.setNoAdherent("01-18100-9");
		response.setNoReference(noRef);
		response.setLigneCodage("042>" + noRef + "+ 010181009>");
		response.setMessage("Le BVR+ a été généré");
		return response;
	}

	private static String buildNoRef(BvrDemande bvrDemande) {
		final StringBuilder b = new StringBuilder();

		// 11 chiffres aléatoires
		for (int i = 0 ; i < 11 ; ++ i) {
			b.append((char) ('0' + RND.nextInt(10)));
		}

		// le numéro de débiteur sur 9 positions avec des zéros en tête
		b.append(String.format("%09d", Integer.parseInt(bvrDemande.getNdc())));

		// 6 fois "0"
		b.append("000000");

		// le chiffre clé
		b.append((char)('0' + CalculateurChiffreCle.getKey(b.toString())));

		return b.toString();
	}

	@Override
	public void ping() throws BVRPlusClientException {
	}
}

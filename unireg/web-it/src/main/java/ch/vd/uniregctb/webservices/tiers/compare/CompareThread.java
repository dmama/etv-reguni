package ch.vd.uniregctb.webservices.tiers.compare;

import org.apache.log4j.Logger;

import ch.vd.interfaces.fiscal.Fiscal;
import ch.vd.interfaces.fiscal.RechercherNoContribuable;
import ch.vd.interfaces.fiscal.RechercherNoContribuableResponse;

/**
 * Thread qui comnpare les resultats des contribuables trouvées.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CompareThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(CompareThread.class);

	private final Fiscal portUnireg;
	private final Fiscal portHost;
	private final RechercherNoContribuable recherche;

	public CompareThread(Fiscal portUnireg, Fiscal portHost, RechercherNoContribuable recherche) {
		this.portUnireg = portUnireg;
		this.portHost = portHost;
		this.recherche = recherche;

		LOGGER.info("[thread-" + this.getId() + "] parametres recherche " + recherche.getNom() + " " + recherche.getPrenom());

	}

	@Override
	public void run() {

		RechercherNoContribuableResponse reponseUnireg = portUnireg.rechercherNoContribuable(recherche);
		RechercherNoContribuableResponse reponseHost = portHost.rechercherNoContribuable(recherche);
		compareReponse(reponseUnireg, reponseHost);
	}

	private void compareReponse(RechercherNoContribuableResponse reponseUnireg, RechercherNoContribuableResponse reponseHost) {
		LOGGER.info("Debut de recherche de difference pour le contribuable "+ reponseUnireg.getNoContribuableSeul());
		if (reponseUnireg.getNbrOccurence() != reponseHost.getNbrOccurence()) {
			LOGGER.info("nombre occurence different: Unireg: " + reponseUnireg.getNbrOccurence() + " Host: "
							+ reponseHost.getNbrOccurence());
		}
		if (reponseUnireg.getNoContribuableCouple() != reponseHost.getNoContribuableCouple()) {
			LOGGER.info("numero couple different: Unireg: " + reponseUnireg.getNoContribuableCouple() + " Host: "
							+ reponseHost.getNoContribuableCouple());
		}
		if (reponseUnireg.getNoContribuableSeul() != reponseHost.getNoContribuableSeul()) {
			LOGGER.info("numero seul different: Unireg: " + reponseUnireg.getNoContribuableSeul() + " Host: "
							+ reponseHost.getNoContribuableSeul());
		}
		if (reponseUnireg.isSourcierPur()!= reponseHost.isSourcierPur()) {
			LOGGER.info("Sourcier pur different: Unireg: " + reponseUnireg.isSourcierPur() + " Host: "
							+ reponseHost.isSourcierPur());
		}
		LOGGER.info("Fin de recherche de difference pour le contribuable "+ reponseUnireg.getNoContribuableSeul());
	}
}

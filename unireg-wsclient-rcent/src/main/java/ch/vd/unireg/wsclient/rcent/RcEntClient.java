package ch.vd.unireg.wsclient.rcent;

import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.evd0022.v1.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;

/**
 * Interface du client RCEnt
 */
public interface RcEntClient {

	/**
	 * Qualification de l'état de l'organisation demandé, dans le temps par rapport
	 * au moment d'une annonce ou dans l'absolu.
	 */
	enum OrganisationState {
		BEFORE("before"), AFTER("after"), CURRENT("current");

		private String value;

		OrganisationState(String value) {
			this.value = value;
		}

		public String toString() {
			return this.value;
		}
	}

	/**
	 * @param id l'identifiant (cantonal) de l'entreprise ou établissement
	 * @param referenceDate une date de référence (ignorée si l'historique est demandé, date du jour si non assignée et historique non-demandé)
	 * @param withHistory <code>true</code> pour obtenir l'historique de l'organisation
	 * @return les données retournées par RCEnt
	 * @throws RcEntClientException en cas de problème
	 */
	OrganisationData getOrganisation(long id, RegDate referenceDate, boolean withHistory) throws RcEntClientException;

	/**
	 * Obtenir les entreprises concernées par une annonce, avec leurs données. Permet de retrouver le contenu d'une annonce RCEnt.
	 *
	 * Le paramètre when précise quel état doit être renvoyé, et peut prendre 3 valeurs:
	 *  - "current": Renvoie l'état
	 *
	 * @param noticeId identifiant de l'annonce RCEnt concernée
	 * @param when Paramètre optionel précisant quel état doit être retourné.
	 * @return les données retournées par RCEnt
	 * @throws RcEntClientException en cas de problème
	 */
	OrganisationsOfNotice getOrganisationsOfNotice(long noticeId, OrganisationState when) throws RcEntClientException;

	/**
	 * Envoi un message de ping dans le tuyau, afin de s'assurer que RCEnt est bien là...
	 * @throws RcEntClientException en cas de souci (= RCEnt n'est pas en état de nous répondre...)
	 */
	void ping() throws RcEntClientException;
}

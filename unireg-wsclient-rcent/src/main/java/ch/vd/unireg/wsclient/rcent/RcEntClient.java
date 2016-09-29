package ch.vd.unireg.wsclient.rcent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0023.v3.ListOfNoticeRequest;
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
	 * @param id            l'identifiant (cantonal) de l'entreprise ou établissement
	 * @param referenceDate une date de référence (ignorée si l'historique est demandé, date du jour si non assignée et historique non-demandé)
	 * @param withHistory   <code>true</code> pour obtenir l'historique de l'organisation
	 * @return les données retournées par RCEnt
	 * @throws RcEntClientException en cas de problème
	 */
	OrganisationData getOrganisation(long id, RegDate referenceDate, boolean withHistory) throws RcEntClientException;

	/**
	 * Obtenir les entreprises concernées par une annonce, avec leurs données. Permet de retrouver le contenu d'une annonce RCEnt.
	 * <p>
	 * Le paramètre when précise quel état doit être renvoyé, et peut prendre 3 valeurs:
	 * - "current": Renvoie l'état
	 *
	 * @param noticeId identifiant de l'annonce RCEnt concernée
	 * @param when     Paramètre optionel précisant quel état doit être retourné.
	 * @return les données retournées par RCEnt
	 * @throws RcEntClientException en cas de problème
	 */
	OrganisationsOfNotice getOrganisationsOfNotice(long noticeId, OrganisationState when) throws RcEntClientException;

	/**
	 * @param noide         numéro IDE au format canonique (= sans point ni tirets, juste les 3 lettres et les 9 chiffres)
	 * @param referenceDate une date de référence (ignorée si l'historique est demandé, date du jour si non-assignée et historique non-demandé)
	 * @param withHistory   <code>true</code> pour obtenir les historiques des organisations trouvées
	 * @return les données retournées par RCEnt (une erreur 404 renvoyée par RCEnt est mappée en un retour de <code>null</code>)
	 * @throws RcEntClientException en cas de problème
	 */
	@Nullable
	OrganisationData getOrganisationByNoIDE(String noide, RegDate referenceDate, boolean withHistory) throws RcEntClientException;

	/**
	 * Méthode à appeler pour valider le contenu d'une demande d'annonce. Cette validation est obligatoire en préalable à
	 * l'envoi de la demande proprement dite via l'esb.
	 *
	 * @param noticeRequest la demande d'annonce à valider
	 * @return un rapport de demande d'annonce contenant éventuellement une liste d'erreurs
	 * @throws RcEntClientException en cas de problème
	 */
	NoticeRequestReport validateNoticeRequest(NoticeRequest noticeRequest) throws RcEntClientException;

	/**
	 * Récupère une demande d'annonce, par son numéro.
	 *
	 * @param noticeRequestId le numéro
	 * @return la demande d'annonce
	 * @throws RcEntClientException en cas de problème
	 */
	ListOfNoticeRequest getNoticeRequest(String noticeRequestId) throws RcEntClientException;

	/**
	 * Recherche des demandes d'annonces sur les entreprises.
	 *
	 * @param query          les critères de recherche des annonces
	 * @param order          l'ordre de tri demandé pour les résultats
	 * @param pageNumber     le numéro de page demandée (1-based)
	 * @param resultsPerPage le nombre d'éléments par page
	 * @return une page avec les annonces correspondantes
	 */
	Page<NoticeRequestReport> findNotices(@NotNull RcEntNoticeQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws RcEntClientException;

	/**
	 * Envoi un message de ping dans le tuyau, afin de s'assurer que RCEnt est bien là...
	 *
	 * @throws RcEntClientException en cas de souci (= RCEnt n'est pas en état de nous répondre...)
	 */
	void ping() throws RcEntClientException;
}

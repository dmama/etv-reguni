package ch.vd.unireg.wsclient.rcent;

import java.util.List;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.evd0022.v1.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;

/**
 * Interface du client RCEnt
 */
public interface RcEntClient {

	/**
	 * @param id l'identifiant (cantonal) de l'entreprise ou établissement
	 * @param referenceDate une date de référence (ignorée si l'historique est demandé, date du jour si non assignée et historique non-demandé)
	 * @param withHistory <code>true</code> pour obtenir l'historique de l'organisation
	 * @return les données retournées par RCEnt
	 * @throws RcEntClientException en cas de problème
	 */
	OrganisationData getOrganisation(long id, RegDate referenceDate, boolean withHistory) throws RcEntClientException;

	/**
	 * @param noticeId identifiant d'une annonce RCEnt
	 * @return les organisations concernées par cette annonce, dans leur état juste avant l'annonce
	 * @throws RcEntClientException en cas de problème
	 */
	OrganisationsOfNotice getOrganisationsBeforeNotice(long noticeId) throws RcEntClientException;

	/**
	 * @param noticeId identifiant d'une annonce RCEnt
	 * @return les organisations concernées par cette annonce, dans leur état juste après l'annonce
	 * @throws RcEntClientException en cas de problème
	 */
	OrganisationsOfNotice getOrganisationsAfterNotice(long noticeId) throws RcEntClientException;
}

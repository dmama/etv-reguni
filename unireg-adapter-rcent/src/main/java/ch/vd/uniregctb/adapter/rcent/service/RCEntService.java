package ch.vd.uniregctb.adapter.rcent.service;


import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.evd0022.v1.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.uniregctb.adapter.rcent.historizer.OrganisationHistorizer;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

/**
 * Adapteur / abstraction de service pour RC-ENT. Expose les requêtes dont nous avons besoin.
 */
public class RCEntService {

	private RcEntClient rcentClient;
	private OrganisationHistorizer historizer;

	public RCEntService(RcEntClient rcentClient, OrganisationHistorizer historizer) {
		this.rcentClient = rcentClient;
		this.historizer = historizer;
	}

	/**
	 * Recherche l'état d'organisation aujourd'hui.
	 *
	 * @param id Identifiant cantonal de l'organisation
	 * @return les données retournées par RCEnt
	 */
	public Organisation getOrganisation(long id) {
		OrganisationData data = rcentClient.getOrganisation(id, RegDate.get(), false);
		return historizer.mapOrganisation(data.getOrganisationSnapshot());
    }

	/**
	 * Recherche l'état d'une organisation à la date indiquée.
	 *
	 * @param id Identifiant cantonal de l'organisation
	 * @param date la date. Optionel. Comportement par défaut de RCEnt si null.
	 * @return les données retournées par RCEnt
	 */
	public Organisation getOrganisation(long id, RegDate date) {
		OrganisationData data = rcentClient.getOrganisation(id, date, false);
		return historizer.mapOrganisation(data.getOrganisationSnapshot());
	}

	/**
	 * Recherche tous les états d'une organisation.
	 *
	 * @param id Identifiant cantonal de l'organisation
	 * @return les données retournées par RCEnt
	 */
	public Organisation getOrganisationHistory(long id) {
		OrganisationData data = rcentClient.getOrganisation(id, null, true);
		return historizer.mapOrganisation(data.getOrganisationSnapshot());
	}

	/**
	 * Recherche l'état d'un établissement aujourd'hui.
	 *
	 * NOTE: La structure renvoiée est bien celle d'une organisation mais qui ne contient QUE
	 * l'établissement demandé.
	 * @param id
	 * @return
	 */
    public OrganisationLocation getLocation(Long id) {
	    OrganisationData data = rcentClient.getOrganisation(id, RegDate.get(), false);
	    return historizer.mapOrganisation(data.getOrganisationSnapshot()).getLocationData().stream().findFirst().orElse(null);
    }

	/**
	 * Recherche l'état d'un établissement à la date indiquée.
	 *
	 * NOTE: La structure renvoiée est bien celle d'une organisation mais qui ne contient QUE
	 * l'établissement demandé.
	 * @param id
	 * @param date
	 * @return
	 */
	public OrganisationLocation getLocation(Long id, RegDate date) {
		OrganisationData data = rcentClient.getOrganisation(id, date, false);
		return historizer.mapOrganisation(data.getOrganisationSnapshot()).getLocationData().stream().findFirst().orElse(null);
	}

	/**
	 * Recherche tous les états d'un établissement.
	 *
	 * NOTE: La structure renvoiée est bien celle d'une organisation mais qui ne contient QUE
	 * l'établissement demandé.
	 * @param id
	 * @return
	 */
	public OrganisationLocation getLocationHistory(Long id) {
		OrganisationData data = rcentClient.getOrganisation(id, null, true);
		return historizer.mapOrganisation(data.getOrganisationSnapshot()).getLocationData().stream().findFirst().orElse(null);
	}

	/**
	 * Recherche de l'état d'organisations juste avant l'annonce qui les concerne.
	 *
	 * @param noticeId
	 * @return
	 * @throws RcEntClientException
	 */
	public OrganisationsOfNotice getOrganisationsBeforeNotice(long noticeId) throws RcEntClientException {
		return rcentClient.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.BEFORE);
	}

	/**
	 * Recherche de l'état d'organisations juste après l'annonce qui les concerne.
	 *
	 * @param noticeId
	 * @return
	 * @throws RcEntClientException
	 */
	public OrganisationsOfNotice getOrganisationsAfterNotice(long noticeId) throws RcEntClientException {
		return rcentClient.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.AFTER);
	}

	/**
	 * Recherche de l'état actuel d'organisations concernées par l'annonce.
	 *
	 * @param noticeId
	 * @return
	 * @throws RcEntClientException
	 */
	public OrganisationsOfNotice getOrganisationsFromNoticeAsOfNow(long noticeId) throws RcEntClientException {
		return rcentClient.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.CURRENT);
	}
}

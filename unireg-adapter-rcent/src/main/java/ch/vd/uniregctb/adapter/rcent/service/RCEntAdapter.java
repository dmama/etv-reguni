package ch.vd.uniregctb.adapter.rcent.service;


import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.uniregctb.adapter.rcent.historizer.OrganisationHistorizer;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;

/**
 * Adapteur / abstraction de service pour RC-ENT. Expose les requêtes dont nous avons besoin.
 */
public class RCEntAdapter {

	private RcEntClient rcentClient;
	private OrganisationHistorizer historizer;

	public RCEntAdapter(RcEntClient rcentClient, OrganisationHistorizer historizer) {
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
    public Organisation getLocation(Long id) {
	    OrganisationData data = rcentClient.getOrganisation(id, RegDate.get(), false);
	    return historizer.mapOrganisation(data.getOrganisationSnapshot());
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
	public Organisation getLocation(Long id, RegDate date) {
		OrganisationData data = rcentClient.getOrganisation(id, date, false);
		return historizer.mapOrganisation(data.getOrganisationSnapshot());
	}

	/**
	 * Recherche tous les états d'un établissement.
	 *
	 * NOTE: La structure renvoiée est bien celle d'une organisation mais qui ne contient QUE
	 * l'établissement demandé.
	 * @param id
	 * @return
	 */
	public Organisation getLocationHistory(Long id) {
		OrganisationData data = rcentClient.getOrganisation(id, null, true);
		return historizer.mapOrganisation(data.getOrganisationSnapshot());
	}
}

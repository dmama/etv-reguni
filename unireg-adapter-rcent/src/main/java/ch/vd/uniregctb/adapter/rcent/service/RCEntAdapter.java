package ch.vd.uniregctb.adapter.rcent.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.NoticeOrganisation;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationSnapshot;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientErrorMessage;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.uniregctb.adapter.rcent.historizer.OrganisationHistorizer;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;

/**
 * Adapteur / abstraction de service pour RC-ENT. Expose les requêtes dont nous avons besoin.
 */
public class RCEntAdapter {

	private static final int RCENT_ERROR_NO_DATA_BEFORE = 9;
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
	 * Récupère l'état à la date indiquée de l'entreprise connue par son numéro IDE
	 * @param noide le numéro IDE en question (format sans points ni tiret)
	 * @param date date de référence (on prendra la date du jour si <code>null</code>)
	 * @return les données retournées par RCEnt (<b>attention !</b> si le numéro IDE est spécifique à un établissement secondaire de l'entreprise dans RCEnt,
	 *         alors l'organisation retournée ne comprendra pas les autres établissements secondaires de l'entreprise)
	 */
	@Nullable
	public Organisation getOrganisationByNoIde(String noide, RegDate date) {
		final OrganisationData data = rcentClient.getOrganisationByNoIDE(noide, date, false);
		return data != null ? historizer.mapOrganisation(data.getOrganisationSnapshot()) : null;
	}

	/**
	 * Récupère l'historique des états de l'entreprise connue par son numéro IDE
	 * @param noide le numéro IDE en question (format sans points ni tiret)
	 * @return les données retournées par RCEnt (<b>attention !</b> si le numéro IDE est spécifique à un établissement secondaire de l'entreprise dans RCEnt,
	 *         alors l'organisation retournée ne comprendra pas les autres établissements secondaires de l'entreprise)
	 */
	@Nullable
	public Organisation getOrganisationHistoryByNoIde(String noide) {
		final OrganisationData data = rcentClient.getOrganisationByNoIDE(noide, null, true);
		return data != null ? historizer.mapOrganisation(data.getOrganisationSnapshot()) : null;
	}

	/**
	 * Recherche les états avant et après de l'événement RCEnt et contruit le pseudo historique correspondant.
	 *
 	 * @param eventId Identifiant de l'événement RCEnt
	 * @return les pseudo historiques des organisations touchées par l'événement, indexés par no cantonal d'organisation
	 */
	public Map<Long, Organisation> getPseudoHistoryForEvent(long eventId) {
		Map<Long, Organisation> resultMap = new HashMap<>();
		Map<Long, NoticeOrganisation> beforeMap = new HashMap<>();
		Map<Long, NoticeOrganisation> afterMap = new HashMap<>();

		OrganisationsOfNotice before;
		OrganisationsOfNotice after = rcentClient.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER);
		RegDate evtDate = after.getNotice().getNoticeDate();
		/*
		 Si on recoit une ou plusieurs erreur 9, on ignore car cela indique une absence de données due à l'arrivée dans RCEnt.
		  */
		before = null;
		try {
			before = rcentClient.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE);
		} catch (RcEntClientException e) {
			boolean real_error = false;
			if (!e.getErrors().isEmpty()) {
				for (RcEntClientErrorMessage error : e.getErrors()) {
					if (error.getCode() != RCENT_ERROR_NO_DATA_BEFORE) {
						real_error = true;
						break;
					}
				}
			}
			if (real_error) {
				throw e;
			}
		}

		// Collecter les identifiants de toutes les organisations touchées
		if (before != null) {
			for (NoticeOrganisation noticeOrg : before.getOrganisation()) {
				final long cantonalId = noticeOrg.getOrganisation().getCantonalId().longValue();
				resultMap.put(cantonalId, null);
				beforeMap.put(cantonalId, noticeOrg);
			}
		}
		for (NoticeOrganisation noticeOrg : after.getOrganisation()) {
			final long cantonalId = noticeOrg.getOrganisation().getCantonalId().longValue();
			resultMap.put(cantonalId, null);
			afterMap.put(cantonalId, noticeOrg);
		}
		// Pour chaque organisation, passer les données avant/après dans l'historizer
		for (Long cantonalId : resultMap.keySet()) {
			List<OrganisationSnapshot> pseudoSnapshots = new ArrayList<>();
			OrganisationSnapshot snapBefore;
			final NoticeOrganisation noticeOrganisationBefore = beforeMap.get(cantonalId);
			if (noticeOrganisationBefore != null) {
				snapBefore = new OrganisationSnapshot(evtDate.getOneDayBefore(), noticeOrganisationBefore.getOrganisation());
				pseudoSnapshots.add(snapBefore);
			}
			OrganisationSnapshot snapAfter = new OrganisationSnapshot(evtDate, afterMap.get(cantonalId).getOrganisation());
			pseudoSnapshots.add(snapAfter);
			resultMap.put(cantonalId, historizer.mapOrganisation(pseudoSnapshots));
		}

		return resultMap;
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

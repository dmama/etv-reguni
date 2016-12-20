package ch.vd.uniregctb.adapter.rcent.service;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.CommercialRegisterData;
import ch.vd.evd0022.v3.CommercialRegisterDiaryEntry;
import ch.vd.evd0022.v3.NoticeOrganisation;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.evd0022.v3.OrganisationSnapshot;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientErrorMessage;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.uniregctb.adapter.rcent.historizer.OrganisationHistorizer;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationEvent;

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
	 * les pseudo historiques des organisations touchées par l'événement, indexés par no cantonal d'organisation
	 *
 	 * @param eventId Identifiant de l'événement RCEnt
	 * @return une map d'événements, un par organisation touchée par l'événement orginal
	 */
	public Map<Long, OrganisationEvent> getOrganisationEvent(long eventId) {
		Map<Long, OrganisationEvent> resultMap = new HashMap<>();
		Map<Long, NoticeOrganisation> beforeMap = new HashMap<>();
		Map<Long, NoticeOrganisation> afterMap = new HashMap<>();

		OrganisationsOfNotice before;
		OrganisationsOfNotice after = rcentClient.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER);
		RegDate evtDate = after.getNotice().getNoticeDate();
		Long targetLocationId = after.getOrganisation().get(0).getOrganisationLocationIdentification().getCantonalId().longValue();

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
			} else {
				throw e;
			}
			if (real_error) {
				throw e;
			}
		}

		// Collecter les snapshots et les identifiants de toutes les organisations touchées
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

		// Pour chaque organisation, passer les données avant/après dans l'historizer et créer l'objet de résultat avec les metadonnées
		for (Long cantonalId : resultMap.keySet()) {
			// Génération du snapshot "avant", si applicable
			OrganisationSnapshot snapBefore = null;
			final NoticeOrganisation noticeOrganisationBefore = beforeMap.get(cantonalId);
			if (noticeOrganisationBefore != null) {
				snapBefore = new OrganisationSnapshot(evtDate.getOneDayBefore(), noticeOrganisationBefore.getOrganisation());
			}
			// Génération du snapshot "après"
			OrganisationSnapshot snapAfter = new OrganisationSnapshot(evtDate, afterMap.get(cantonalId).getOrganisation());

			// Constitution de la liste de snapshot, analogue à ce que renvoie le WS Organisation
			List<OrganisationSnapshot> pseudoSnapshots = snapBefore == null ? Collections.singletonList(snapAfter) : Arrays.asList(snapBefore, snapAfter);
			// Passage à travers l'historizer et récupération de l'historique généré. Ce n'est qu'un historique très partiel centré autour de l'événement.
			final Organisation pseudoHistory = historizer.mapOrganisation(pseudoSnapshots);

			// Constitution de l'objet représentant l'événement pour cet organisation
			final OrganisationEvent eventResult = new OrganisationEvent(eventId, targetLocationId, pseudoHistory);

			// Ajout des métadonnées. NOTE: actuellement, l'établissement cible est unique pour un evt. RCEnt. Comme on sépare un événement RCEnt en
			// autant qu'il en faut pour chaque organisation, l'établissement cible peut ne pas être représenté dans l'événement en cours d'analyse, car
			// faisant partie d'une autre organisation.

			// Trouver les entrées de journal de l'événement pour le jour, avant et après. Cela implique de trouver le site cible de l'événement.
			final ch.vd.evd0022.v3.OrganisationLocation locationBefore = getTargetLocation(before, targetLocationId);
			final List<CommercialRegisterDiaryEntry> diaryEntriesBefore = getEntriesForTheDay(locationBefore, evtDate);

			final ch.vd.evd0022.v3.OrganisationLocation locationAfter = getTargetLocation(after, targetLocationId);
			if (locationAfter == null) {
				throw new RCEntAdapterException(String.format("The OrganisationLocation no %s targeted by this RCEnt event cannot be found in the event's data!", targetLocationId));
			}
			final List<CommercialRegisterDiaryEntry> diaryEntriesAfter = getEntriesForTheDay(locationAfter, evtDate);

			final CommercialRegisterDiaryEntry logEntryRC;

			if (!diaryEntriesAfter.isEmpty()) {
				// Exactement une entrée pour le jour, c'est si simple
				if (diaryEntriesAfter.size() == 1) {
					logEntryRC = diaryEntriesAfter.get(0);
				}
				// Exactement une entrée de moins avant qu'après pour le jour, on peut encore déterminer laquelle est à l'origine de l'événement.
				else if (diaryEntriesBefore.size() == diaryEntriesAfter.size() - 1) {

					// La nouvelle entrée est celle qui est présente dans la nouvelle liste mais pas l'ancienne
					final List<CommercialRegisterDiaryEntry> entryForEvt = diaryEntriesAfter.stream()
							.filter(e ->
									        diaryEntriesBefore.stream()
											        .filter(eb -> eb.getDiaryEntryNumber().equals(e.getDiaryEntryNumber()))
											        .collect(Collectors.toList()).size() == 0
							)
							.collect(Collectors.toList());
					if (entryForEvt.size() != 1) {
						// On a un problème, les deux listes ne se correspondent pas de tel sorte que l'une est identique à l'autre + 1 entrée.
						// Tant pis. Si RCEnt n'est pas capabe de fournir quelque chose de cohérent, qu'y peut-on?
						logEntryRC = null;
					}
					else {
						// Chic, on a trouvé l'entrée nouvelle
						logEntryRC = entryForEvt.get(0);
					}
				}
				else {
					logEntryRC = null;
				}
				// Pas d'entrée de journal, avant/après sont identiques (si ce n'est pas le cas, c'est qu'il y a un problème, mais ce n'est pas notre problème).

				if (logEntryRC != null) {
					eventResult.setCommercialRegisterEntryNumber(logEntryRC.getDiaryEntryNumber().longValue());
					eventResult.setCommercialRegisterEntryDate(logEntryRC.getDiaryEntryDate());
					eventResult.setDocumentNumberFOSC(logEntryRC.getSwissGazetteOfCommercePublication().getDocumentNumber());
					eventResult.setPublicationDateFOSC(logEntryRC.getSwissGazetteOfCommercePublication().getPublicationDate());
				}
			}
			else {
				// On n'a pas d'entrée de journal RC, mais on a une publication FOSC peut-être? On recherche selon le même principe.
				BusinessPublication businessPublication;

				final List<BusinessPublication> publicationsBefore = getPublicationsForTheDay(locationBefore, evtDate);
				final List<BusinessPublication> publicationsAfter = getPublicationsForTheDay(locationAfter, evtDate);


				if (publicationsAfter.size() == 1) {
					businessPublication = publicationsAfter.get(0);
				}
				else if (publicationsBefore.size() == publicationsAfter.size() - 1) {
					final List<BusinessPublication> publicationForEvt = publicationsAfter.stream()
							.filter(e ->
									        publicationsBefore.stream()
											        .filter(eb -> eb.getSwissGazetteOfCommercePublication().getDocumentNumber().equals(e.getSwissGazetteOfCommercePublication().getDocumentNumber()))
											        .collect(Collectors.toList()).size() == 0
							)
							.collect(Collectors.toList());
					if (publicationForEvt.size() != 1) {
						businessPublication = null;
					}
					else {
						businessPublication = publicationForEvt.get(0);
					}
				} else {
					businessPublication = null;
				}
				if (businessPublication != null) {
					eventResult.setDocumentNumberFOSC(businessPublication.getSwissGazetteOfCommercePublication().getDocumentNumber());
					eventResult.setPublicationDateFOSC(businessPublication.getSwissGazetteOfCommercePublication().getPublicationDate());
				}
			}

			resultMap.put(cantonalId, eventResult);
		}

		return resultMap;
	}

	private List<CommercialRegisterDiaryEntry> getEntriesForTheDay(@Nullable OrganisationLocation organisationLocationAfter, RegDate evtDate) {
		if (organisationLocationAfter == null) {
			return Collections.emptyList();
		}
		final CommercialRegisterData commercialRegisterData = organisationLocationAfter.getCommercialRegisterData();
		if (commercialRegisterData == null) {
			return Collections.emptyList();
		}
		return commercialRegisterData
				.getDiaryEntry().stream()
				.filter(e -> RegDateHelper.equals(e.getSwissGazetteOfCommercePublication().getPublicationDate(), evtDate))
				.collect(Collectors.toList());
	}

	private ch.vd.evd0022.v3.OrganisationLocation getTargetLocation(@Nullable  OrganisationsOfNotice notice, Long targetLocationId) {
		if (notice == null) {
			return null;
		}

		return notice.getOrganisation().stream()
				.map(NoticeOrganisation::getOrganisation)
				.flatMap(organisation -> organisation.getOrganisationLocation().stream())
				.filter(l -> l.getCantonalId().longValue() == targetLocationId)
				.findFirst().orElseGet(() -> null);
	}

	private List<BusinessPublication> getPublicationsForTheDay(@Nullable OrganisationLocation location, RegDate evtDate) {
		if (location == null) {
			return Collections.emptyList();
		}
		final List<BusinessPublication> businessPublication = location.getBusinessPublication();
		if (businessPublication == null) {
			return Collections.emptyList();
		}
		return businessPublication.stream()
				.filter(publication -> publication.getSwissGazetteOfCommercePublication().getPublicationDate() == evtDate)
				.collect(Collectors.toList());
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

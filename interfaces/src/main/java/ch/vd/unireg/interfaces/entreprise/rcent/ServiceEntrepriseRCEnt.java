package ch.vd.unireg.interfaces.entreprise.rcent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.PaginationHelper;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseRaw;
import ch.vd.unireg.interfaces.entreprise.WrongEntrepriseReceivedException;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseConstants;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.OrganisationEvent;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.service.RCEntAdapter;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;

public class ServiceEntrepriseRCEnt implements ServiceEntrepriseRaw {

	private final RcEntClient client;
	private final RCEntAdapter adapter;
	private final ServiceInfrastructureRaw infraService;
	public static final Logger LOGGER = LoggerFactory.getLogger(ServiceEntrepriseRCEnt.class);

	public ServiceEntrepriseRCEnt(RCEntAdapter adapter, RcEntClient client, ServiceInfrastructureRaw infraService) {
		this.adapter = adapter;
		this.client = client;
		this.infraService = infraService;
	}

	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws ServiceEntrepriseException {
		try {
			final ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.Organisation received = adapter.getOrganisationHistory(noEntreprise);
			if (received == null) {
				return null;
			}
			sanityCheck(noEntreprise, received.getCantonalId());
			return RCEntOrganisationHelper.get(received, infraService);
		}
		catch (RcEntClientException e) {
			throw new ServiceEntrepriseException(e);
		}
	}

	@Override
	public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws ServiceEntrepriseException {
		try {
			final ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.Organisation received = adapter.getLocation(noEtablissementCivil);
			if (received == null) {
				return null;
			}
			return received.getCantonalId();
		}
		catch (RcEntClientException e) {
			throw new ServiceEntrepriseException(e);
		}
	}

	@Override
	public Identifiers getEntrepriseByNoIde(String noide) throws ServiceEntrepriseException {
		try {
			final ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.Organisation received = adapter.getOrganisationByNoIde(noide, null);
			if (received == null) {
				return null;
			}

			for (ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.OrganisationLocation location : received.getLocationData()) {
				final List<DateRangeHelper.Ranged<String>> candidatsIde = location.getIdentifiers().get(EntrepriseConstants.CLE_IDE);
				if (candidatsIde != null) {
					for (DateRangeHelper.Ranged<String> candidatIde : candidatsIde) {
						if (noide != null && noide.equals(candidatIde.getPayload())) {
							return new Identifiers(received.getCantonalId(), location.getCantonalId());
						}
					}
				}
			}
		}
		catch (RcEntClientException e) {
			throw new ServiceEntrepriseException(e);
		}

		// super bizarre... on nous renvoie des données mais ces données ne contiennent pas l'information recherchée...
		throw new ServiceEntrepriseException(String.format("Les données renvoyées par RCEnt pour le numéro IDE %s ne contiennent aucun établissement arborant ce numéro.",
		                                                   noide));
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws ServiceEntrepriseException {
		try {
			final Map<Long, ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.OrganisationEvent> received = adapter.getOrganisationEvent(noEvenement);
			if (received == null || received.isEmpty()) {
				return Collections.emptyMap();
			}
			final Map<Long, EntrepriseCivileEvent> result = new HashMap<>(received.size());
			for (Map.Entry<Long, ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.OrganisationEvent> orgEntry : received.entrySet()) {
				final OrganisationEvent organisationEvent = orgEntry.getValue();
				final EntrepriseCivileEvent entrepriseCivileEvent = new EntrepriseCivileEvent(
						organisationEvent.getEventNumber(),
						organisationEvent.getTargetLocationId(),
						RCEntOrganisationHelper.get(organisationEvent.getPseudoHistory(), infraService)
				);
				entrepriseCivileEvent.setNumeroEntreeJournalRC(organisationEvent.getCommercialRegisterEntryNumber());
				entrepriseCivileEvent.setDateEntreeJournalRC(organisationEvent.getCommercialRegisterEntryDate());
				if (!StringUtils.isBlank(organisationEvent.getDocumentNumberFOSC())) {
					entrepriseCivileEvent.setNumeroDocumentFOSC(organisationEvent.getDocumentNumberFOSC());
				}
				entrepriseCivileEvent.setDatePublicationFOSC(organisationEvent.getPublicationDateFOSC());
				result.put(orgEntry.getKey(), entrepriseCivileEvent);
			}
			return result;
		}
		catch (RcEntClientException e) {
			throw new ServiceEntrepriseException(e);
		}
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {

		try {
			final Sort sort = (order == null ? null : new Sort(order));
			final PageRequest pageable = new PageRequest(pageNumber, resultsPerPage, sort);

			// on fait la requête au client
			final Page<NoticeRequestReport> notices = client.findNotices(query.toFindNoticeQuery(), order, pageNumber + 1, resultsPerPage);
			if (notices == null) {
				return new PageImpl<>(Collections.<AnnonceIDE>emptyList(), pageable, 0);
			}
			else {
				// on adapte les réponses
				// SIFISC-27766 on ignore les annonces REE pour l'instant
				final List<AnnonceIDE> annonces = notices.getContent().stream()
						.filter(n -> n.getNoticeRequest().getNoticeRequestHeader() != null)
						.map(RCEntAnnonceIDEHelper::buildAnnonceIDE)
						.collect(Collectors.toList());
				return PaginationHelper.buildPage(annonces, pageNumber, resultsPerPage, notices.getTotalElements(), sort);
			}
		}
		catch (RcEntClientException e) {
			throw new ServiceEntrepriseException(e);
		}
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceEntrepriseException {
		if (modele == null) {
			throw new IllegalArgumentException("Modèle d'annonce à valider manquant!");
		}

		final NoticeRequest noticeRequest = RCEntAnnonceIDEHelper.buildNoticeRequest(modele);
		try {
			final NoticeRequestReport noticeReport = client.validateNoticeRequest(noticeRequest);
			if (noticeReport == null || noticeReport.getNoticeRequest() == null) {
				final BaseAnnonceIDE.Contenu contenu = modele.getContenu();
				throw new ServiceEntrepriseException(String.format("Reçu une réponse vide lors de l'appel pour valider le modèle d'annonce à l'IDE (entreprise: %s)", contenu == null ? "" : contenu.getNom()));
			}
			final BaseAnnonceIDE.Statut statut = RCEntAnnonceIDEHelper.buildProtoAnnonceIDE(noticeReport).getStatut();
			cleanErreurs(statut);
			return statut;
		}
		catch (RcEntClientException e) {
			throw new ServiceEntrepriseException(e);
		}
	}

	/**
	 * Enlever les erreurs vides qui existent comme décrit dans SIREF-9354 même lorsqu'il n'y a pas d'erreur. Les erreurs
	 * sont enlevée <strong>directement</strong> de la liste qui compose le statut.
	 *
	 * @param statut l'objet statut
	 */
	private void cleanErreurs(BaseAnnonceIDE.Statut statut) {
		if (statut != null) {
			final List<Pair<String, String>> erreurs = statut.getErreurs();
			if (erreurs != null && !erreurs.isEmpty()) {
				final List<Pair<String, String>> aEnlever = new ArrayList<>(erreurs.size());
				for (Pair<String, String> erreur : erreurs) {
					if (StringUtils.isBlank(erreur.getLeft()) && StringUtils.isBlank(erreur.getRight())) {
						aEnlever.add(erreur);
					}
				}
				erreurs.removeAll(aEnlever);
			}
		}
	}

	@Override
	public void ping() throws ServiceEntrepriseException {
		try {
			client.ping();
		}
		catch (RcEntClientException e) {
			throw new ServiceEntrepriseException(e);
		}
	}

	private void sanityCheck(long noEntrepriseCivile, long receivedId) throws ServiceEntrepriseException {
		if (receivedId != noEntrepriseCivile) {
			throw new WrongEntrepriseReceivedException(noEntrepriseCivile, receivedId);
		}
	}
}

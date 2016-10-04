package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0023.v3.ListOfNoticeRequest;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.WrongOrganisationReceivedException;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationConstants;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationEvent;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;

public class ServiceOrganisationRCEnt implements ServiceOrganisationRaw {

	private final RcEntClient client;
	private final RCEntAdapter adapter;
	private final ServiceInfrastructureRaw infraService;

	public ServiceOrganisationRCEnt(RCEntAdapter adapter, RcEntClient client, ServiceInfrastructureRaw infraService) {
		this.adapter = adapter;
		this.client = client;
		this.infraService = infraService;
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		try {
			final ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisationHistory(noOrganisation);
			if (received == null) {
				return null;
			}
			sanityCheck(noOrganisation, received.getCantonalId());
			return RCEntOrganisationHelper.get(received, infraService);
		}
		catch (RcEntClientException e) {
			throw new ServiceOrganisationException(e);
		}
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		try {
			final ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getLocation(noSite);
			if (received == null) {
				return null;
			}
			return received.getCantonalId();
		}
		catch (RcEntClientException e) {
			throw new ServiceOrganisationException(e);
		}
	}

	@Override
	public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		try {
			final ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisationByNoIde(noide, null);
			if (received == null) {
				return null;
			}

			for (ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation location : received.getLocationData()) {
				final List<DateRangeHelper.Ranged<String>> candidatsIde = location.getIdentifiers().get(OrganisationConstants.CLE_IDE);
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
			throw new ServiceOrganisationException(e);
		}

		// super bizarre... on nous renvoie des données mais ces données ne contiennent pas l'information recherchée...
		throw new ServiceOrganisationException(String.format("Les données renvoyées par RCEnt pour le numéro IDE %s ne contiennent aucun établissement arborant ce numéro.",
		                                                     noide));
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		try {
			final Map<Long, ch.vd.uniregctb.adapter.rcent.model.OrganisationEvent> received = adapter.getOrganisationEvent(noEvenement);
			if (received == null || received.isEmpty()) {
				return Collections.emptyMap();
			}
			final Map<Long, ServiceOrganisationEvent> result = new HashMap<>(received.size());
			for (Map.Entry<Long, ch.vd.uniregctb.adapter.rcent.model.OrganisationEvent> orgEntry : received.entrySet()) {
				final OrganisationEvent organisationEvent = orgEntry.getValue();
				final ServiceOrganisationEvent serviceOrganisationEvent = new ServiceOrganisationEvent(
						organisationEvent.getEventNumber(),
						organisationEvent.getTargetLocationId(),
						RCEntOrganisationHelper.get(organisationEvent.getPseudoHistory(), infraService)
				);
				serviceOrganisationEvent.setNumeroEntreeJournalRC(organisationEvent.getCommercialRegisterEntryNumber());
				serviceOrganisationEvent.setDateEntreeJournalRC(organisationEvent.getCommercialRegisterEntryDate());
				if (!StringUtils.isBlank(organisationEvent.getDocumentNumberFOSC())) {
					serviceOrganisationEvent.setNumeroDocumentFOSC(Long.parseLong(organisationEvent.getDocumentNumberFOSC()));
				}
				serviceOrganisationEvent.setDatePublicationFOSC(organisationEvent.getPublicationDateFOSC());
				result.put(orgEntry.getKey(), serviceOrganisationEvent);
			}
			return result;
		}
		catch (RcEntClientException e) {
			throw new ServiceOrganisationException(e);
		}
	}

	@Override
	public AnnonceIDEEnvoyee getAnnonceIDE(long numero) throws ServiceOrganisationException {
		final ListOfNoticeRequest noticeRequests = client.getNoticeRequest(String.valueOf(numero));
		if (noticeRequests == null || noticeRequests.getNumberOfResults() == 0) {
			return null;
		} else if (noticeRequests.getNumberOfResults() > 2) {
			throw new ServiceOrganisationException("La recherche de l'annonce par son id (" + String.valueOf(numero) + ") a renvoyé plusieurs résultats!");
		}
		// FIXME (raphaël) ce cast est interdit !
		return (AnnonceIDEEnvoyee) RCEntAnnonceIDEHelper.get(noticeRequests.getResults().get(0));
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {

		final Sort sort = (order == null ? null : new Sort(order));
		final PageRequest pageable = new PageRequest(pageNumber, resultsPerPage, sort);

		// on fait la requête au client
		final Page<NoticeRequestReport> notices = client.findNotices(query.toFindNoticeQuery(), order, pageNumber + 1, resultsPerPage);
		if (notices == null) {
			return new PageImpl<AnnonceIDE>(Collections.<AnnonceIDE>emptyList(), pageable, 0);
		}
		else {
			// on adapte les réponses
			final List<AnnonceIDE> annonces = new ArrayList<>(notices.getNumberOfElements());
			for (NoticeRequestReport n : notices.getContent()) {
				final ProtoAnnonceIDE modele = RCEntAnnonceIDEHelper.get(n);

				// FIXME (msi) hack pour transformer un modèle en annonce. A corriger quand Raphaël aura committé son cleanup
				final long numero = Long.parseLong(n.getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId());
				final AnnonceIDE a = new AnnonceIDE(numero, modele, modele.getStatut());
				annonces.add(a);
			}
			return new PageImpl<AnnonceIDE>(annonces, pageable, notices.getTotalElements());
		}
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceOrganisationException {
		Assert.notNull(modele, "Modèle d'annonce à valider manquant!");

		final NoticeRequest noticeRequest = RCEntAnnonceIDEHelper.buildNoticeRequest(modele);
		final NoticeRequestReport noticeReport = client.validateNoticeRequest(noticeRequest);
		if (noticeReport == null || noticeReport.getNoticeRequest() == null) {
			final BaseAnnonceIDE.Contenu contenu = modele.getContenu();
			throw new ServiceOrganisationException(String.format("Reçu une réponse vide lors de l'appel pour valider le modèle d'annonce IDE (entreprise: %s)", contenu == null ? "" : contenu.getNom()));
		}
		final BaseAnnonceIDE.Statut statut = RCEntAnnonceIDEHelper.get(noticeReport).getStatut();
		cleanErreurs(statut);
		return statut;
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
					if (StringUtils.isBlank(erreur.getFirst()) && StringUtils.isBlank(erreur.getSecond())) {
						aEnlever.add(erreur);
					}
				}
				for (Pair<String, String> erreur : aEnlever) {
					erreurs.remove(erreur);
				}
			}
		}
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		try {
			client.ping();
		}
		catch (RcEntClientException e) {
			throw new ServiceOrganisationException(e);
		}
	}

	private void sanityCheck(long noOrganisation, long receivedId) throws ServiceOrganisationException {
		if (receivedId != noOrganisation) {
			throw new WrongOrganisationReceivedException(noOrganisation, receivedId);
		}
	}
}

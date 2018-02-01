package ch.vd.unireg.registrefoncier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.unireg.evenement.identification.contribuable.Demande;
import ch.vd.unireg.evenement.identification.contribuable.EsbHeader;
import ch.vd.unireg.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.Reponse;
import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TypeTiers;

public class RapprochementManuelTiersRFServiceImpl implements RapprochementManuelTiersRFService {

	/**
	 * Type du message d'identification créé quand un rapprochement n'a pas pu être fait de manière automatique
	 */
	public static final String TYPE_MESSAGE_IDENTIFICATION = "RapprochementTiersRF";

	private HibernateTemplate hibernateTemplate;
	private IdentCtbDAO identCtbDAO;
	private String queueRetourPourIdentification;
	private String emetteurDemandeIdentification;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setQueueRetourPourIdentification(String queueRetourPourIdentification) {
		this.queueRetourPourIdentification = queueRetourPourIdentification;
	}

	public void setEmetteurDemandeIdentification(String emetteurDemandeIdentification) {
		this.emetteurDemandeIdentification = emetteurDemandeIdentification;
	}

	@Override
	public void genererDemandeIdentificationManuelle(TiersRF tiersRF) {
		if (!demandeATraiterExiste(tiersRF)) {
			creerDemandeIdentificationManuelle(tiersRF);
		}
	}

	@Override
	public void marquerDemandesIdentificationManuelleEventuelles(TiersRF tiersRF, Tiers tiersUnireg) {
		getEncoreATraiter(tiersRF).forEach(i -> {
			final Reponse reponse = new Reponse();
			reponse.setDate(DateHelper.getCurrentDate());
			reponse.setNoContribuable(tiersUnireg.getNumero());

			i.setEtat(IdentificationContribuable.Etat.TRAITE_AUTOMATIQUEMENT);
			i.setCommentaireTraitement(null);
			i.setNbContribuablesTrouves(1);
			i.setReponse(reponse);
			i.setDateTraitement(DateHelper.getCurrentDate());
			i.setTraitementUser(AuthenticationHelper.getCurrentPrincipal());

			// normalement, quand on fait ça, il faudrait envoyer le message de réponse au service demandeur
			// mais ici, comme on traite des messages typés "rapprochement RF", Unireg est le service demandeur
			// lui-même, donc pas besoin d'envoyer un quelconque message...
		});
	}

	private boolean demandeATraiterExiste(TiersRF tiersRF) {
		return getEncoreATraiter(tiersRF).findFirst().isPresent();
	}

	private Stream<IdentificationContribuable> getEncoreATraiter(TiersRF tiersRF) {
		final List<IdentificationContribuable> found = identCtbDAO.find(TypeDemande.RAPPROCHEMENT_RF, emetteurDemandeIdentification, String.format("%d ", tiersRF.getId()));
		return found.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(identif -> identif.getEtat().isEncoreATraiter());
	}

	private void creerDemandeIdentificationManuelle(TiersRF tiersRF) {

		final CriteresPersonne personne = extractCriteresPersonne(tiersRF);

		final Demande demande = new Demande();
		demande.setDate(DateHelper.getCurrentDate());
		demande.setEmetteurId(emetteurDemandeIdentification);
		demande.setMessageId(UUID.randomUUID().toString());
		demande.setModeIdentification(Demande.ModeIdentificationType.MANUEL_SANS_ACK);
		demande.setPersonne(personne);
		demande.setPrioriteEmetteur(Demande.PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setTransmetteur(null);
		demande.setTypeDemande(TypeDemande.RAPPROCHEMENT_RF);
		demande.setTypeMessage(TYPE_MESSAGE_IDENTIFICATION);
		demande.setPeriodeFiscale(DateHelper.getCurrentYear());
		demande.setTypeContribuableRecherche(extractTypeTiersRecherche(tiersRF));

		final EsbHeader header = new EsbHeader();
		header.setBusinessId(String.format("%d %d", tiersRF.getId(), demande.getDate().getTime()));
		header.setMetadata(Collections.singletonMap(ID_TIERS_RF, String.valueOf(tiersRF.getId())));
		header.setBusinessUser(AuthenticationHelper.getCurrentPrincipal());
		header.setReplyTo(queueRetourPourIdentification);

		final IdentificationContribuable pseudoMessage = new IdentificationContribuable();
		pseudoMessage.setDemande(demande);
		pseudoMessage.setEtat(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT);
		pseudoMessage.setHeader(header);

		// sauvegarde en base
		hibernateTemplate.merge(pseudoMessage);
	}

	private static TypeTiers extractTypeTiersRecherche(TiersRF tiersRF) {
		if (tiersRF instanceof PersonnePhysiqueRF) {
			return TypeTiers.PERSONNE_PHYSIQUE;
		}
		else {
			return TypeTiers.ENTREPRISE;
		}
	}

	@NotNull
	private static CriteresPersonne extractCriteresPersonne(TiersRF tiersRF) {
		final CriteresPersonne criteres;
		if (tiersRF instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) tiersRF;
			criteres = new CriteresPersonne();
			criteres.setNom(pp.getNom());
			criteres.setPrenoms(pp.getPrenom());
			criteres.setDateNaissance(pp.getDateNaissance());
		}
		else if (tiersRF instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) tiersRF;
			criteres = new CriteresPersonne();
			criteres.setNom(pm.getRaisonSociale());
		}
		else if (tiersRF instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF cp = (CollectivitePubliqueRF) tiersRF;
			criteres = new CriteresPersonne();
			criteres.setNom(cp.getRaisonSociale());
		}
		else {
			throw new IllegalArgumentException("Type de tiers RF inconnu...");
		}
		return criteres;
	}
}

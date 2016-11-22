package ch.vd.uniregctb.registrefoncier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.EsbHeader;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.hibernate.HibernateTemplate;

public class RapprochementManuelTiersRFServiceImpl implements RapprochementManuelTiersRFService {

	/**
	 * Nom de l'attribut placé dans les headers du message d'identification
	 * (qui sera donc retourné à l'appelant)
	 */
	public static final String ID_TIERS_RF = "idTiersRF";

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

	private boolean demandeATraiterExiste(TiersRF tiersRF) {
		final List<IdentificationContribuable> found = identCtbDAO.find(TypeDemande.RAPPROCHEMENT_RF, emetteurDemandeIdentification, String.format("%d ", tiersRF.getId()));
		return found != null && found.stream()
				.map(IdentificationContribuable::getEtat)
				.filter(IdentificationContribuable.Etat::isEncoreATraiter)
				.findFirst()
				.isPresent();
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

		// TODO ne manque-t-il pas un type de tiers à rechercher ?

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

	@NotNull
	private CriteresPersonne extractCriteresPersonne(TiersRF tiersRF) {
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

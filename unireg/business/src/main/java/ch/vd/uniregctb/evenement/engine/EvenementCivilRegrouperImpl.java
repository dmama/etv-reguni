package ch.vd.uniregctb.evenement.engine;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupeDAO;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaireDAO;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Classe permettant le regroupement des evenements civils.
 *
 * @author zsicpi
 */
public class EvenementCivilRegrouperImpl implements EvenementCivilRegrouper {

	private static Logger LOGGER = Logger.getLogger(EvenementCivilRegrouperImpl.class);

	private EvenementCivilUnitaireDAO evenementCivilUnitaireDAO = null;
	private EvenementCivilRegroupeDAO evenementCivilRegroupeDAO = null;
	private TiersDAO tiersDAO = null;
	private ServiceCivilService serviceCivilService = null;
	private PlatformTransactionManager transactionManager;


	public void regroupeTousEvenementsNonTraites() {
		List<EvenementCivilUnitaire> evenements = evenementCivilUnitaireDAO.findNotTreatedEvenementsOrderByDate();
		regroupeEvenementsList(evenements);
	}

	/**
	 * Regroupe l'evt unitaire passé en paramètre
	 *
	 * @param l'ID de l'evt unitaire
	 * @return l'ID de l'evt regroupé
	 */
	private void regroupeEvenementsList(List<EvenementCivilUnitaire> evenements) {

		LOGGER.debug("Regroupement des événements civils unitaires...");

		int nbEvenementsTraites = 0;
		int nbEvenementsEnErreurs = 0;
		/* On itère sur tous les événements civils unitaires non traités */
		for (EvenementCivilUnitaire evenement : evenements) {
			long id = regroupeUnEvenementById(evenement.getId(), null);
			if (id < 0) {
				nbEvenementsEnErreurs++;
			}
			nbEvenementsTraites++;
		}
		LOGGER.debug("Regroupement des événements civils unitaires terminé Traités: " + nbEvenementsTraites + " Erreurs: " + nbEvenementsEnErreurs);
	}

	/**
	 * Crée une nouvelle transaction pour rendre atomique le regroupement d'un événement.
	 *
	 * @param l'ID de l'evt unitaire
	 * @return l'ID de l'evt regroupé, -1 en cas d'erreur
	 */
	public long regroupeUnEvenementById(final long id, final StringBuffer errorMsg) {

		TransactionCallback callback = new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				EvenementCivilUnitaire unitaire = evenementCivilUnitaireDAO.get(id);

				Assert.notNull(unitaire);
				Assert.notNull(unitaire.getId());
				Assert.notNull(unitaire.getType());
				Assert.isTrue(unitaire.getEtat() != EtatEvenementCivil.TRAITE);

				//Audit.info(unitaire.getId(), "Regroupement de l'événement "+unitaire.getId()+" de type "+unitaire.getType().toString());

				long ret = 0L;
				/* tentative de regroupement */
				try {
					ret = doRegroupeEvenement(unitaire);
				}
				catch (Exception e) {
					if (errorMsg != null) {
						errorMsg.append(e.getMessage());
					}
					Audit.error(e.getMessage());
					ret = -1L;
				}

				/* Mise à jour de l'état de l'évènement unitaire */
				if (ret < 0) {
					unitaire.setEtat(EtatEvenementCivil.EN_ERREUR);
				}
				else {
					unitaire.setEtat(EtatEvenementCivil.TRAITE);
				}

				/* on retour le statut d'erreur */
				return ret;
			}
		};

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		Long ret = -1L;
		try {
			ret = (Long)template.execute(callback);
		}
		finally {
			if (ret < 0) {
				Audit.error(id, "Le regroupement de l'événement unitaire a produit une erreur");
			}
		}
		return ret;
	}

	/**
	 * Regroupe les evenements civil nouveau
	 *
	 * @return -1 en cas d'erreur, l'ID de l'evt regroupe sinon
	 */
	private long doRegroupeEvenement(EvenementCivilUnitaire evenement) throws Exception {

		Assert.notNull(evenement.getDateEvenement(), "La date est invalide");

		// on signale que l'individu à changé dans le registre civil (=> va rafraîchir le cache des individus)
		serviceCivilService.onIndividuChange(evenement.getNumeroIndividu());

		/* Récupération de l'individu et de sons conjoint */
		Individu individu = serviceCivilService.getIndividu(evenement.getNumeroIndividu(), RegDateHelper.getAnneeVeille(evenement.getDateEvenement()), new EnumAttributeIndividu[] { EnumAttributeIndividu.CONJOINT });
		Assert.notNull(individu, "Individu inconnu");

		List<EvenementCivilRegroupe> evenements = null;
		EvenementCivilRegroupe evRegroupe = null;

		/* Marié/pacsé ou autre */
		boolean individuSeul = true;

		Individu conjoint = serviceCivilService.getConjoint(individu.getNoTechnique(), evenement.getDateEvenement());
		if (conjoint != null) {

			// on signale que l'individu à changé dans le registre civil (=> va rafraîchir le cache des individus)
			serviceCivilService.onIndividuChange(conjoint.getNoTechnique());

			/* on recherche un evenement rattaché au conjoint, meme date, meme type dont l'état n'est pas TRAITE / A_VERIFIER */
			evenements = evenementCivilRegroupeDAO.rechercheEvenementExistant(evenement.getDateEvenement(), evenement.getType(),
					conjoint.getNoTechnique());

			/* si plusieurs événements trouvés, on met l'événement unitaire en erreur */
			if (evenements.size() > 1) {
				Audit.error(evenement.getId().intValue(), "L'événement de type "+evenement.getType().toString()+" existe plusieurs fois en base de données");
				throw new EvenementCivilHandlerException("Impossible de regrouper l'événement ("+evenement.getId()+") : plusieurs regroupements possible");
			}

			/* si un seul regroupement possible, ok */
			else if (!evenements.isEmpty()) {
				evRegroupe = evenements.get(0);
				evRegroupe.setHabitantConjoint( getHabitant(evenement) );
				evRegroupe.setNumeroIndividuConjoint( evenement.getNumeroIndividu() );
				evRegroupe.setEtat(EtatEvenementCivil.A_TRAITER);
				evRegroupe.getErreurs().clear();	// neutralisation des anciennes erreurs avant le re-traitement (UNIREG-942]
				Audit.info(evRegroupe.getId().intValue(), "Sur l'événement REGROUPE de type "+evRegroupe.getType().toString()+" a été ajouté l'individu "+evenement.getNumeroIndividu());
				individuSeul = false;
			}
		}

		if (individuSeul) {
			// si aucun regroupement possible, ou si Célibataire
			// on crée un nouvel événement regroupé
			evRegroupe = copyEvenementCivilUnitaireToRegroupe(evenement);
			evRegroupe = evenementCivilRegroupeDAO.save(evRegroupe);
			evRegroupe.setHabitantPrincipal( getHabitant(evenement) );
			evRegroupe.setNumeroIndividuPrincipal( evenement.getNumeroIndividu() );
			Audit.info(evRegroupe.getId().intValue(), "L'événement REGROUPE de type "+evRegroupe.getType().toString()+" a été créé sur l'individu "+evenement.getNumeroIndividu());
		}

		/* sauvegarde de l'événement regroupé résultant */
		long id = -1L;
		if ( evRegroupe != null ) {
			id = evRegroupe.getId();
		}

		return id;
	}

	/**
	 * Copie les propriétés d'un evenemenet Unitaire dans un regroupé
	 * @param evenementCivilRegroupe
	 * @return
	 */
	private EvenementCivilRegroupe copyEvenementCivilUnitaireToRegroupe(EvenementCivilUnitaire unitaire) {

		EvenementCivilRegroupe regroupe = new EvenementCivilRegroupe();
		// On garde l'ID de l'evt unitaire
		regroupe.setId(unitaire.getId());
		regroupe.setDateEvenement(unitaire.getDateEvenement());
		regroupe.setDateTraitement(new Date());
		regroupe.setNumeroOfsCommuneAnnonce(unitaire.getNumeroOfsCommuneAnnonce());
		regroupe.setType(unitaire.getType());
		regroupe.setEtat(EtatEvenementCivil.A_TRAITER);
		return regroupe;
	}

	/**
	 * Renvoie l'habitant rattaché à cet événement.
	 * Si l'habitant n'existe pas, il en crée un en base
	 * @param 	evenement 	l'événement dont on veut l'habitant
	 * @return
	 */
	private PersonnePhysique getHabitant(EvenementCivilUnitaire evenement) {

		/* on recherche l'habitant (ou ancien habitant...) correspondant au numero d'individu present dans l'evenement */
		return tiersDAO.getPPByNumeroIndividu( evenement.getNumeroIndividu() );
	}

	/**
	 *
	 * @param serviceCivil
	 */
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	/**
	 *
	 * @param evenementCivilUnitaireDAO
	 */
	public void setEvenementCivilUnitaireDAO(EvenementCivilUnitaireDAO evenementCivilUnitaireDAO) {
		this.evenementCivilUnitaireDAO = evenementCivilUnitaireDAO;
	}

	/**
	 *
	 * @param evenementCivilRegroupeDAO
	 */
	public void setEvenementCivilRegroupeDAO(EvenementCivilRegroupeDAO evenementCivilRegroupeDAO) {
		this.evenementCivilRegroupeDAO = evenementCivilRegroupeDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}

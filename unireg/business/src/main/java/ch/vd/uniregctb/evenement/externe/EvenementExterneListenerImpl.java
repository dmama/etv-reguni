package ch.vd.uniregctb.evenement.externe;

import java.util.Date;

import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 *
 * @author xcicfh
 *
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class EvenementExterneListenerImpl implements EvenementExterneListener {

	private EvenementExterneDAO evenementExterneDAO;

	private TiersDAO tiersDAO;

	private DatabaseService databaseService;

	public EvenementExterneListenerImpl() {

	}

	/**
	 * @param evenementExterneDAO
	 *            the evenementExterneDAO to set
	 */
	public void setEvenementExterneDAO(EvenementExterneDAO evenementExterneDAO) {
		this.evenementExterneDAO = evenementExterneDAO;
	}

	/**
	 * @param tiersDAO
	 *            the tiersDAO to set
	 */
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDatabaseService(DatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	/**
	 *
	 * @param quittance
	 * @throws Exception
	 */
	private DeclarationImpotSource validate(Tiers tiers, EvenementImpotSourceQuittanceType quittance) throws Exception {
		DeclarationImpotSource declarationImpotSource = null;
		// La période de décompte a été ouverte et n’a pas été annulée.
		for (Declaration declaration : tiers.getDeclarations()) {
			if (declaration instanceof DeclarationImpotSource && !declaration.isAnnule()) {
				RegDate debut = RegDate.get(quittance.getDateDebutPeriode().getTime());
				if (declaration.getDateDebut().compareTo(debut) == 0) {
					declarationImpotSource = (DeclarationImpotSource) declaration;
					break;
				}
			}
		}
		if (declarationImpotSource == null) {
			throw new Exception("Il n'est pas de déclaration impôt source pour ce débiteur: " + tiers.getNumero());
		}
		// Au moins la date de retour ou l’annulation du retour doit être renseignée et elles ne peuvent être présentes
		// simultanément.
		if (quittance.getTypeQuittance() == TypeQuittance.QUITTANCEMENT) {
			if (quittance.getDateQuittance() == null) {
				throw new Exception("Pour un quittancement la date de retour est requise.");
			}
			// Si la date de retour est renseignée, elle ne se situe pas dans le futur et le retour n’a pas encore été enregistré.
			if (DateHelper.isAfter(quittance.getDateQuittance().getTime(), new Date())) {
				throw new Exception("La date de retour ne peut se situer dans le futur");
			}
		}
		else if (quittance.getTypeQuittance() == TypeQuittance.ANNULATION) {
			if (quittance.getDateQuittance() != null) {
				throw new Exception("Pour une annulation la date de retour ne doit pas être renseignée.");
			}
			EtatDeclaration etatDeclaration = declarationImpotSource.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
			// S’il s’agit d’une annulation du retour, le retour a déjà été enregistré.
			if (etatDeclaration == null) {
				throw new Exception("La déclaration impôt source sélectionné ne contient pas de retour à annuler.");
			}
		}

		return declarationImpotSource;
	}

	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void quittancementLr(Tiers tiers, EvenementImpotSourceQuittanceType quittance) throws Exception {
		final DeclarationImpotSource declarationImpotSource = validate(tiers, quittance);
		// En l’absence d’erreur, l’application met à jour la liste récapitulative du débiteur de l’impôt à la source
		// correspondant à la période de décompte :
		// ● Si la date de retour est renseignée, elle en y insère la date de retour.
		// ● S’il s’agit d’une annulation du retour, elle efface la date de retour.
		final TypeQuittance.Enum quitancement = quittance.getTypeQuittance();
		if (TypeQuittance.QUITTANCEMENT.equals(quitancement)) {
			EtatDeclaration etatDeclaration = new EtatDeclaration();
			etatDeclaration.setEtat(TypeEtatDeclaration.RETOURNEE);
			etatDeclaration.setDateObtention(RegDate.get(quittance.getDateQuittance().getTime()));
			etatDeclaration.setAnnule(false);
			declarationImpotSource.addEtat(etatDeclaration);
		}
		else if (TypeQuittance.ANNULATION.equals(quitancement)) {
			final EtatDeclaration etatDeclaration = declarationImpotSource.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
			etatDeclaration.setAnnule(true);
		}
		else {
			throw new RuntimeException("Unexpected Error.");
		}

		// [UNIREG-1947] EvenementExterneListenerImplne pas oublier d'invalider le cache du tiers!
		databaseService.onTiersChange(tiers.getNumero());
	}

	/**
	 * Pré requis pour le traitement des événements d'impôt source
	 *
	 * @param evenementImpotSource
	 * @throws Exception
	 */
	private Tiers validateTiers(EvenementImpotSourceType evenementImpotSource) throws Exception {
		// Le débiteur de l’impôt à la source doit exister et être actif,
		final Tiers tiers = tiersDAO.get(Long.parseLong(evenementImpotSource.getNumeroTiers()));
		if (tiers == null) {
			throw new Exception("Tiers n'existe pas " + evenementImpotSource.getNumeroTiers());
		}
		return tiers;
	}

	/**
	 *
	 * @param evenementImpotSource
	 * @param event
	 * @return
	 * @throws Exception
	 */
	private EvenementExterne saveEvent(EvenementImpotSourceType evenementImpotSource, EvenementExterneReceivedEvent event) throws Exception {
		EvenementExterne evenementExterne = evenementExterneDAO.creerEvenementExterne(event.getText(), event.getCorrelationId());
		Tiers tiers = null;
		try {
			tiers = validateTiers(evenementImpotSource);
		}
		catch (Exception e) {
			evenementExterne = evenementExterneDAO.save(evenementExterne);
			this.evenementExterneDAO.traceEvenementEnError(evenementExterne.getId(), e.getMessage());
			throw new Exception(e.getMessage(), e);
		}
		evenementExterne.setTiers(tiers);
		evenementExterne = evenementExterneDAO.save(evenementExterne);
		// recharge l'événement hors de la transaction REQUIRES_NEW de la fonction save().
		return evenementExterneDAO.get(evenementExterne.getId());
	}

	/**
	 *
	 * @param evenementExterne
	 * @param evenementImpotSource
	 * @throws Exception
	 */
	private void processEvenement(EvenementExterne evenementExterne, EvenementImpotSourceType evenementImpotSource) throws Exception {
		final Tiers tiers = evenementExterne.getTiers();
		try {
			if (evenementImpotSource instanceof EvenementImpotSourceQuittanceType) {
				quittancementLr(tiers, (EvenementImpotSourceQuittanceType) evenementImpotSource);
			}
			else {
				Audit.error("Evenement Externe de " + EmmetteurType.ImpotSource + " n'est pas reconnu.");
			}
		}
		catch (Exception exception) {
			throw new Exception(exception.getMessage(), exception);
		}
	}

	/**
	 *
	 * @param event
	 */
	protected void doEvent(EvenementExterneReceivedEvent event) {
		if (EmmetteurType.ImpotSource == event.getEmmetteur()) {
			EvenementImpotSourceType evenementImpotSource = (EvenementImpotSourceType) event.getEvenement();
			//
			if (evenementExterneDAO.existe(event.getCorrelationId())) {
				return;
			}
			if (event.hasError()) {
				Audit.error("Reception Evenement Externe de " + EmmetteurType.ImpotSource + ": Unexpected Error " + event.getError());
			}
			else {
				EvenementExterne evenementExterne = null;
				try {
					evenementExterne = saveEvent(evenementImpotSource, event);
					processEvenement(evenementExterne, evenementImpotSource);
					this.evenementExterneDAO.traceEvenementTraite(evenementExterne.getId());
				}
				catch (Exception e) {
					if (evenementExterne != null) {
						this.evenementExterneDAO.traceEvenementEnError(evenementExterne.getId(), e.getMessage());
					} else {
						throw new RuntimeException(e);
					}

				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof EvenementExterneReceivedEvent) {
			doEvent((EvenementExterneReceivedEvent) event);
		}

	}

}

package ch.vd.uniregctb.evenement.externe;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.hibernate.interceptor.ModificationLogInterceptor;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EvenementExterneServiceImpl implements EvenementExterneService, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementExterneServiceImpl.class);

	private EvenementExterneSender sender;
	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private ModificationLogInterceptor modifInterceptor;
	private DataEventService dataEventService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSender(EvenementExterneSender sender) {
		this.sender = sender;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementExterneDAO(EvenementExterneDAO evenementExterneDAO) {
		this.evenementExterneDAO = evenementExterneDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setModifInterceptor(ModificationLogInterceptor modifInterceptor) {
		this.modifInterceptor = modifInterceptor;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendEvent(String businessId, EvenementImpotSourceQuittanceDocument document) throws Exception {
		sender.sendEvent(businessId, document);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param event
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void onEvent(EvenementExterne event) throws EvenementExterneException {

		if (evenementExterneDAO.existe(event.getBusinessId())) {
			LOGGER.warn("Le message avec le business id=[" + event.getBusinessId() + "] existe déjà en base: il est ignoré.");
			return;
		}

		if (event instanceof QuittanceLR) {
			onQuittance((QuittanceLR) event);
		}
		else {
			throw new EvenementExterneException("Type d'événement inconnu = " + event.getClass());
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param event
	 */
	@Transactional(rollbackFor = Throwable.class)
	public int traiterEvenementExterne(EvenementExterne event) throws EvenementExterneException {
		int resultat = 0;
		if (event instanceof QuittanceLR) {
			resultat = traiterQuittanceLR((QuittanceLR) event);
		}
		else {
			throw new EvenementExterneException("Type d'événement inconnu = " + event.getClass());
		}
		return resultat;
	}

	private int traiterQuittanceLR(QuittanceLR event) throws EvenementExterneException {
		final Long tiersId = event.getTiersId();
		int resultat = 0;
		// On recherche le débiteur correspondant
		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			throw new EvenementExterneException("Tiers n'existe pas " + tiersId);
		}

		// Si la cible de l'evenement(la LR) est déjà traitée, on ne fait rien
		if (lrDejaTraitee(event, tiers)) {
			resultat = 0;
		}
		else {
			// On quittance la LR
			quittancementLr(event);
			// Tout s'est bien passé
			event.setEtat(EtatEvenementExterne.TRAITE);
			event.setErrorMessage(null);
			resultat = 1;
		}
		return resultat;


	}

	/**
	 * teste si la lr a u etat conforme pour le type de quittancement que l'evenement demande:
	 * si la LR a un etat retourne et que l'on a une demande de quittancement
	 * ou si la lr a un etat non retourne et
	 * * que l'on a une demande d'annulation de quittancement la lr est déja traité, donc on est sensé ne rien faire
	 *
	 * @param event
	 * @param tiers
	 * @return
	 * @throws EvenementExterneException
	 */
	//
	private boolean lrDejaTraitee(QuittanceLR event, Tiers tiers) throws EvenementExterneException {
		boolean etatConforme = true;
		final RegDate dateValidite = event.getDateFin().getOneDayBefore();
		Declaration lr = tiers.getDeclarationActive(dateValidite);
		if (lr != null) {
			if (TypeEtatDeclaration.RETOURNEE.equals(lr.getDernierEtat()) && TypeQuittance.QUITTANCEMENT.equals(event.getType()) ||
					isNonQuittancee(lr) && TypeQuittance.ANNULATION.equals(event.getType())) {
				etatConforme = false;
			}
		}
		else {
			throw new EvenementExterneException("la LR " + event.getDateDebut() + "-" + event.getDateFin() + " du debiteur " + tiers.getNumero() + " n'existe pas");
		}

		return etatConforme;
	}

	private boolean isNonQuittancee(Declaration lr) {
		if (TypeEtatDeclaration.RETOURNEE.equals(lr.getDernierEtat())) {
			return false;
		}
		return true;
	}

	private void onQuittance(QuittanceLR event) {

		final Long tiersId = event.getTiersId();

		// Première chose à faire : sauver le message pour allouer un id technique.
		event.setEtat(EtatEvenementExterne.NON_TRAITE);
		event = (QuittanceLR) evenementExterneDAO.save(event);

		try {
			// On recherche le débiteur correspondant
			final Tiers tiers = tiersDAO.get(tiersId);
			if (tiers == null) {
				throw new EvenementExterneException("Tiers n'existe pas " + tiersId);
			}
			event.setTiers(tiers);

			// On quittance la LR
			quittancementLr(event);

			// Tout s'est bien passé
			event.setEtat(EtatEvenementExterne.TRAITE);
			event.setErrorMessage(null);
		}
		catch (EvenementExterneException e) {
			event.setEtat(EtatEvenementExterne.ERREUR);
			event.setErrorMessage(e.getMessage());
		}
	}

	private DeclarationImpotSource validate(QuittanceLR quittance) throws EvenementExterneException {
		DeclarationImpotSource declarationImpotSource = null;
		// La période de décompte a été ouverte et n’a pas été annulée.
		final Tiers tiers = quittance.getTiers();
		for (Declaration declaration : tiers.getDeclarations()) {
			if (declaration instanceof DeclarationImpotSource && !declaration.isAnnule()) {
				if (declaration.getDateDebut() == quittance.getDateDebut()) {
					declarationImpotSource = (DeclarationImpotSource) declaration;
					break;
				}
			}
		}
		if (declarationImpotSource == null) {
			throw new EvenementExterneException("Il n'est pas de déclaration impôt source pour ce débiteur: " + tiers.getNumero());
		}
		// Au moins la date de retour ou l’annulation du retour doit être renseignée et elles ne peuvent être présentes
		// simultanément.
		if (quittance.getType() == TypeQuittance.QUITTANCEMENT) {
			if (quittance.getDateEvenement() == null) {
				throw new EvenementExterneException("Pour un quittancement la date de retour est requise.");
			}
			// Si la date de retour est renseignée, elle ne se situe pas dans le futur et le retour n’a pas encore été enregistré.
			if (DateHelper.isAfter(quittance.getDateEvenement(), new Date())) {
				throw new EvenementExterneException("La date de retour ne peut se situer dans le futur");
			}
		}
		else if (quittance.getType() == TypeQuittance.ANNULATION) {
			if (quittance.getDateEvenement() != null) {
				throw new EvenementExterneException("Pour une annulation la date de retour ne doit pas être renseignée.");
			}
			final EtatDeclaration etatDeclaration = declarationImpotSource.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
			// S’il s’agit d’une annulation du retour, le retour a déjà été enregistré.
			if (etatDeclaration == null) {
				throw new EvenementExterneException("La déclaration impôt source sélectionnée ne contient pas de retour à annuler.");
			}
		}

		return declarationImpotSource;
	}

	private void quittancementLr(QuittanceLR quittance) throws EvenementExterneException {
		final DeclarationImpotSource declarationImpotSource = validate(quittance);
		// En l’absence d’erreur, l’application met à jour la liste récapitulative du débiteur de l’impôt à la source
		// correspondant à la période de décompte :
		// ● Si la date de retour est renseignée, elle en y insère la date de retour.
		// ● S’il s’agit d’une annulation du retour, elle efface la date de retour.
		if (quittance.getType() == TypeQuittance.QUITTANCEMENT) {
			final EtatDeclaration etatDeclaration = new EtatDeclaration();
			etatDeclaration.setEtat(TypeEtatDeclaration.RETOURNEE);
			etatDeclaration.setDateObtention(RegDate.get(quittance.getDateEvenement()));
			etatDeclaration.setAnnule(false);
			declarationImpotSource.addEtat(etatDeclaration);
		}
		else if (quittance.getType() == TypeQuittance.ANNULATION) {
			final EtatDeclaration etatDeclaration = declarationImpotSource.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
			etatDeclaration.setAnnule(true);
		}
		else {
			throw new RuntimeException("Unexpected Error.");
		}

		// [UNIREG-1947] EvenementExterneListenerImplne pas oublier d'invalider le cache du tiers!
		dataEventService.onTiersChange(declarationImpotSource.getTiers().getNumero());
	}

	public EvenementImpotSourceQuittanceDocument createEvenementQuittancement(EvenementImpotSourceQuittanceType.TypeQuittance.Enum quitancement, Long numeroCtb, RegDate dateDebut,
	                                                                          RegDate dateFin, RegDate dateQuittance) {

		Assert.notNull(quitancement, "le type de quittancement est obligation");
		Assert.notNull(numeroCtb, "Le numero du débiteur est obligatoire");
		Assert.notNull(dateDebut, "la date du début du récapitulatif est obligatoire");
		// Assert.assertNotNull(dateFin);

		final EvenementImpotSourceQuittanceDocument doc = EvenementImpotSourceQuittanceDocument.Factory.newInstance();
		final EvenementImpotSourceQuittanceType evenement = doc.addNewEvenementImpotSourceQuittance();
		evenement.setNumeroTiers(numeroCtb.toString());

		final Calendar datedebutC = DateUtils.calendar(dateDebut.asJavaDate());
		evenement.setDateDebutPeriode(datedebutC);
		if (dateFin != null) {
			final Calendar dateFinC = DateUtils.calendar(dateFin.asJavaDate());
			evenement.setDateFinPeriode(dateFinC);
		}

		evenement.setTypeQuittance(quitancement);
		if (quitancement == EvenementImpotSourceQuittanceType.TypeQuittance.QUITTANCEMENT) {
			Assert.notNull(dateQuittance, "la date de quittancement du récapitulatif est obligatoire");
			evenement.setDateQuittance(DateUtils.calendar(dateQuittance.asJavaDate()));
		}
		return doc;
	}

	private static class MigrationResults implements BatchResults<Long, MigrationResults> {

		public final Map<Long, String> erreurs = new HashMap<Long, String>();

		public void addErrorException(Long element, Exception e) {
			erreurs.put(element, e.getMessage());
		}

		public void addAll(MigrationResults right) {
			erreurs.putAll(right.erreurs);
		}
	}

	public void afterPropertiesSet() throws Exception {
		migrateAllQuittancesLR();
	}

	/**
	 * Migre le format des quittances LR.
	 * <p/>
	 * Précisemment, les colonnes <i>date début</i>, <i>date fin</i> et <i>type</i> n'étaient pas renseignées sur les anciennes quittances LR. Par contre, le message xml d'origine (= le contenu du
	 * message JMS) était toujours stocké. La migration ci-dessous reprend donc ces messages, interpète le message xml d'origine et renseigne les colonnes qui vont bien.
	 */
	private void migrateAllQuittancesLR() {
		final List<Long> ids = evenementExterneDAO.getIdsQuittancesLRToMigrate();
		if (!ids.isEmpty()) {

			LOGGER.warn("--- Début de la migration du format de stockage des quittances LR ---");
			MigrationResults rapportFinal = new MigrationResults();

			AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
			modifInterceptor.setCompleteOnly(true); // pour éviter d'écraser les dates de création/modification originales
			try {

				BatchTransactionTemplate<Long, MigrationResults> template =
						new BatchTransactionTemplate<Long, MigrationResults>(ids, 100, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);
				template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, MigrationResults>() {

					@Override
					public MigrationResults createSubRapport() {
						return new MigrationResults();
					}

					@Override
					public boolean doInTransaction(List<Long> batch, MigrationResults rapport) throws Exception {
						migrateQuittancesLR(batch);
						return true;
					}
				});

			}
			finally {
				modifInterceptor.setCompleteOnly(false);
				AuthenticationHelper.popPrincipal();
			}

			for (Map.Entry<Long, String> s : rapportFinal.erreurs.entrySet()) {
				LOGGER.error("Impossible de migrer la quittance LR id=" + s.getKey() + ". Erreur=" + s.getValue());
			}

			LOGGER.warn(
					"--- Fin de la migration (LR migrées=" + (ids.size() - rapportFinal.erreurs.size()) + ", en erreur=" + rapportFinal.erreurs.size() + ") ---");
		}
	}

	private void migrateQuittancesLR(List<Long> ids) throws XmlException {

		for (Long id : ids) {
			final QuittanceLR quittance = (QuittanceLR) evenementExterneDAO.get(id);

			// Récupère le message xml stocké dans la base
			final String bussinessId = quittance.getBusinessId();
			final String message = quittance.getMessage();
			Assert.notNull(message, "Le message est nul.");

			// Interpète le xml et renseigne les colonnes manquantes
			final EvenementExterne completeEvent = EvenementExterneListenerImpl.string2event(message, bussinessId);
			if (completeEvent instanceof QuittanceLR) {
				final QuittanceLR completeQuittance = (QuittanceLR) completeEvent;
				if (quittance.getTiers() == null) {
					final Tiers tiers = tiersDAO.get(completeQuittance.getTiersId());
					quittance.setTiers(tiers);
				}
				quittance.setDateDebut(completeQuittance.getDateDebut());
				quittance.setDateFin(completeQuittance.getDateFin());
				quittance.setType(completeQuittance.getType());
			}
			else {
				throw new IllegalArgumentException("Type d'événement inconnu = " + completeEvent.getClass());
			}
		}
	}
}

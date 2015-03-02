package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationInterceptor;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Processor qui effectue les changements sur les fors fiscaux suite à une fusion de communes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class FusionDeCommunesProcessor {

	private final Logger LOGGER = LoggerFactory.getLogger(OuvertureForsContribuablesMajeursProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final ServiceInfrastructureService serviceInfra;
	private final ValidationService validationService;
	private final ValidationInterceptor validationInterceptor;
	private final AdresseService adresseService;

	private final Map<Class<? extends ForFiscal>, Strategy> strategies = new HashMap<>();

	public FusionDeCommunesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService, ServiceInfrastructureService serviceInfra,
	                                 ValidationService validationService, ValidationInterceptor validationInterceptor, AdresseService adresseService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.serviceInfra = serviceInfra;
		this.validationService = validationService;
		this.validationInterceptor = validationInterceptor;
		this.adresseService = adresseService;

		this.strategies.put(ForFiscalPrincipal.class, new ForPrincipalStrategy());
		this.strategies.put(ForFiscalSecondaire.class, new ForSecondaireStrategy());
		this.strategies.put(ForFiscalAutreElementImposable.class, new ForAutreElementImposableStrategy());
		this.strategies.put(ForDebiteurPrestationImposable.class, new ForDebiteurStrategy());
		this.strategies.put(ForFiscalAutreImpot.class, new ForAutreImpotStrategy());
	}

	/**
	 * Exécute le traitement du processeur à la date de référence spécifiée.
	 *
	 * @param anciensNoOfs   les numéros Ofs des communes qui ont/vont fusionner
	 * @param nouveauNoOfs   le numéro Ofs de la commune résultant de la fusion
	 * @param dateFusion     la date de fusion des communes
	 * @param dateTraitement la date de traitement
	 * @param status         un status manager
	 * @return les résultats détaillés du traitement
	 */
	public FusionDeCommunesResults run(final Set<Integer> anciensNoOfs, final int nouveauNoOfs, final RegDate dateFusion, final RegDate dateTraitement, @Nullable StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		// Vérification de l'existence des commnues
		checkNoOfs(anciensNoOfs, nouveauNoOfs, dateFusion);

		final FusionDeCommunesResults rapportFinal = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
		rapportFinal.phaseTraitement = FusionDeCommunesResults.PhaseTraitement.PHASE_FOR;

		final List<Long> list = getListTiersTouchesParFusion(anciensNoOfs, dateFusion);

		// boucle principale sur les contribuables à traiter
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, FusionDeCommunesResults> template = new BatchTransactionTemplateWithResults<>(list, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, FusionDeCommunesResults>() {

			@Override
			public FusionDeCommunesResults createSubRapport() {
				return new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, FusionDeCommunesResults r) throws Exception {
				s.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiteBatch(batch, anciensNoOfs, nouveauNoOfs, dateFusion, s, r);
				return !s.interrupted();
			}
		}, progressMonitor);

		if (status.interrupted()) {
			status.setMessage("Le traitement de la fusion des communes a été interrompu."
					+ " Nombre de contribuables traités au moment de l'interruption = " + rapportFinal.tiersTraites.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("Le traitement de la fusion des communes est terminé." + " Nombre de contribuables traités = "
					+ rapportFinal.tiersTraites.size() + ". Nombre d'erreurs = " + rapportFinal.tiersEnErrors.size());
		}


		rapportFinal.phaseTraitement = FusionDeCommunesResults.PhaseTraitement.PHASE_DECISION;
		final List<Long> listAvecDecisions = getListTiersAvecDecisionTouchesParFusion(anciensNoOfs, dateFusion);

		// boucle principale sur les contribuables à traiter
		final SimpleProgressMonitor progressMonitorForDecision = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, FusionDeCommunesResults> templateForDecision = new BatchTransactionTemplateWithResults<>(listAvecDecisions, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		templateForDecision.execute(rapportFinal, new BatchWithResultsCallback<Long, FusionDeCommunesResults>() {

			@Override
			public FusionDeCommunesResults createSubRapport() {
				return new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, FusionDeCommunesResults r) throws Exception {
				s.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitorForDecision.getProgressInPercent());
				traiteBatchPourDecision(batch, anciensNoOfs, nouveauNoOfs, dateFusion, s, r);
				return !s.interrupted();
			}
		}, progressMonitorForDecision);

		if (status.interrupted()) {
			status.setMessage("Le traitement de la fusion des communes a été interrompu dans la phase décisions ACI."
					+ " Nombre de contribuables traités au moment de l'interruption = " + rapportFinal.tiersAvecDecisionTraites.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("Le traitement de la fusion des communes est terminé." + " Nombre de contribuables avec décision ACI traités = "
					+ rapportFinal.tiersAvecDecisionTraites.size() + ". Nombre d'erreurs = " + rapportFinal.tiersAvecDecisionEnErrors.size());
		}


		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<Long> batch, final Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, StatusManager s, FusionDeCommunesResults r) {
		for (Long id : batch) {
			traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion, r);
			if (s.interrupted()) {
				break;
			}
		}
	}
	private void traiteBatchPourDecision(List<Long> batch, final Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, StatusManager s, FusionDeCommunesResults r) {
		for (Long id : batch) {
			traiteTiersAvecDecision(id, anciensNoOfs, nouveauNoOfs, dateFusion, r);
			if (s.interrupted()) {
				break;
			}
		}
	}

	protected void traiteTiers(Long id, Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, FusionDeCommunesResults r) {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		Assert.notNull(tiers);
		boolean wasValidationInterceptorEnabled = validationInterceptor.isEnabled();
		try {
			// Desactivation de validation automatique par l'intercepteur
			validationInterceptor.setEnabled(false);

			boolean forIgnore = false;
			boolean forTraite = false;

			final List<ForFiscal> fors = tiers.getForsFiscauxSorted();
			for (ForFiscal f : fors) {

				if (f.isAnnule()) {
					continue;
				}

				// On ne traite que les fors fiscaux correspondant aux communes fusionnées
				if (f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS || !anciensNoOfs.contains(f.getNumeroOfsAutoriteFiscale())) {
					continue;
				}

				// On ne traite que les fors fiscaux valides après la date de la fusion
				if (f.getDateFin() != null && f.getDateFin().isBefore(dateFusion)) {
					continue;
				}

				// On ignore les fors qui sont déjà sur la commune résultant de la fusion
				if (f.getNumeroOfsAutoriteFiscale() == nouveauNoOfs) {
					forIgnore = true;
					continue;
				}

				final Strategy strat = strategies.get(f.getClass());
				if (strat == null) {
					throw new IllegalArgumentException("Type de for fiscal inconnu : " + f.getClass().getSimpleName());
				}

				//noinspection unchecked
				strat.traite(f, nouveauNoOfs, dateFusion);
				forTraite = true;
			}

			// Validation manuelle après le traitement de tous les fors
			final ValidationResults validationResults = validationService.validate(tiers);
			if (validationResults.hasErrors()) {
				throw new ValidationException(tiers, validationResults);
			}

			if (forTraite) {
				r.tiersTraites.add(id);
			}
			else if (forIgnore) {
				r.addTiersIgnoreDejaSurCommuneResultante(tiers);
			}

		}
		catch (RuntimeException e) {
			// on essaie de détecter les erreurs qui pourraient être dues à un tiers qui ne valide pas
			final ValidationResults validationResults = validationService.validate(tiers);
			if (validationResults.hasErrors()) {
				LOGGER.error(String.format("Exception lancée pendant le traitement du tiers %d, qui ne valide pas", tiers.getNumero()), e);
				throw new ValidationException(tiers, validationResults);
			}
			else {
				throw e;
			}
		}
		finally {
			validationInterceptor.setEnabled(wasValidationInterceptorEnabled);
		}
	}

	protected void traiteTiersAvecDecision(Long id, Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, FusionDeCommunesResults r) {

		final Contribuable contribuable = hibernateTemplate.get(Contribuable.class, id);
		Assert.notNull(contribuable);
		boolean wasValidationInterceptorEnabled = validationInterceptor.isEnabled();
		try {
			// Desactivation de validation automatique par l'intercepteur
			validationInterceptor.setEnabled(false);

			boolean decisionIgnore = false;
			boolean decisionTraite = false;

			Set<DecisionAci> decisionAciSet = contribuable.getDecisionsAci();
			final List<DecisionAci> decisions = new ArrayList<>();
			decisions.addAll(decisionAciSet);

			for (DecisionAci d : decisions) {

				if (d.isAnnule()) {
					continue;
				}

				// On ne traite que les décisions correspondantes aux communes fusionnées
				if (d.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS || !anciensNoOfs.contains(d.getNumeroOfsAutoriteFiscale())) {
					continue;
				}

				// On ne traite que les décisions valides après la date de la fusion
				if (d.getDateFin() != null && d.getDateFin().isBefore(dateFusion)) {
					continue;
				}

				// On ignore les décisions qui sont déjà sur la commune résultant de la fusion
				if (d.getNumeroOfsAutoriteFiscale() == nouveauNoOfs) {
					decisionIgnore = true;
					continue;
				}

				traiteDecisionAci(d,nouveauNoOfs,dateFusion);
				decisionTraite = true;
			}

			// Validation manuelle après le traitement de tous les decisions
			final ValidationResults validationResults = validationService.validate(contribuable);
			if (validationResults.hasErrors()) {
				throw new ValidationException(contribuable, validationResults);
			}

			if (decisionTraite) {
				r.tiersAvecDecisionTraites.add(id);
			}
			else if (decisionIgnore) {
				r.addTiersAvecDecisionIgnoreeDejaSurCommuneResultante(contribuable);
			}

		}
		catch (RuntimeException e) {
			// on essaie de détecter les erreurs qui pourraient être dues à un tiers qui ne valide pas
			final ValidationResults validationResults = validationService.validate(contribuable);
			if (validationResults.hasErrors()) {
				LOGGER.error(String.format("Exception lancée pendant le traitement du contribuable %d, qui ne valide pas", contribuable.getNumero()), e);
				throw new ValidationException(contribuable, validationResults);
			}
			else {
				throw e;
			}
		}
		finally {
			validationInterceptor.setEnabled(wasValidationInterceptorEnabled);
		}
	}


	/**
	 * Stratégie de traitement d'un for fiscal dans le cas d'une fusion de communes.
	 *
	 * @param <F> le type concret de for fiscal.
	 */
	private abstract class Strategy<F extends ForFiscal> {
		abstract void traite(F forFiscal, int nouveauNoOfs, RegDate dateFusion);
	}

	private class ForPrincipalStrategy extends Strategy<ForFiscalPrincipal> {
		@Override
		void traite(ForFiscalPrincipal principal, int nouveauNoOfs, RegDate dateFusion) {
			if (principal.getDateDebut().isAfterOrEqual(dateFusion)) {
				// le for débute après la fusion -> on met simplement à jour le numéro Ofs
				principal.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				tiersService.closeForFiscalPrincipal(principal, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES);
				tiersService.openForFiscalPrincipal((Contribuable) principal.getTiers(), dateFusion, principal.getMotifRattachement(), nouveauNoOfs,
						principal.getTypeAutoriteFiscale(), principal.getModeImposition(), MotifFor.FUSION_COMMUNES);
			}
		}
	}

	private class ForSecondaireStrategy extends Strategy<ForFiscalSecondaire> {
		@Override
		void traite(ForFiscalSecondaire secondaire, int nouveauNoOfs, RegDate dateFusion) {
			if (secondaire.getDateDebut().isAfterOrEqual(dateFusion)) {
				// le for débute après la fusion -> on met simplement à jour le numéro Ofs
				secondaire.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				final Contribuable contribuable = (Contribuable) secondaire.getTiers();
				final RegDate dateFinExistante = secondaire.getDateFin();
				final MotifFor motifFermetureExistant = secondaire.getMotifFermeture();
				tiersService.closeForFiscalSecondaire(contribuable, secondaire, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES);
				tiersService.addForSecondaire(contribuable, dateFusion, dateFinExistante, secondaire.getMotifRattachement(),
						nouveauNoOfs, secondaire.getTypeAutoriteFiscale(), MotifFor.FUSION_COMMUNES, motifFermetureExistant);
			}
		}
	}

	private class ForAutreElementImposableStrategy extends Strategy<ForFiscalAutreElementImposable> {
		@Override
		void traite(ForFiscalAutreElementImposable autre, int nouveauNoOfs, RegDate dateFusion) {
			if (autre.getDateDebut().isAfterOrEqual(dateFusion)) {
				// le for débute après la fusion -> on met simplement à jour le numéro Ofs
				autre.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				final Contribuable contribuable = (Contribuable) autre.getTiers();
				tiersService.closeForFiscalAutreElementImposable(contribuable, autre, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES);
				tiersService.openForFiscalAutreElementImposable(contribuable, autre.getGenreImpot(), dateFusion, autre.getMotifRattachement(), nouveauNoOfs,
				                                                MotifFor.FUSION_COMMUNES);
			}
		}
	}

	private class ForDebiteurStrategy extends Strategy<ForDebiteurPrestationImposable> {
		@Override
		void traite(ForDebiteurPrestationImposable deb, int nouveauNoOfs, RegDate dateFusion) {
			if (deb.getDateDebut().isAfterOrEqual(dateFusion)) {
				// le for débute après la fusion -> on met simplement à jour le numéro Ofs
				deb.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) deb.getTiers();
				tiersService.closeForDebiteurPrestationImposable(debiteur, deb, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES, false);
				tiersService.openForDebiteurPrestationImposable(debiteur, dateFusion, MotifFor.FUSION_COMMUNES, nouveauNoOfs, deb.getTypeAutoriteFiscale());
			}
		}
	}

	private class ForAutreImpotStrategy extends Strategy<ForFiscalAutreImpot> {
		@Override
		void traite(ForFiscalAutreImpot autre, int nouveauNoOfs, RegDate dateFusion) {
			if (autre.getDateDebut().isAfterOrEqual(dateFusion)) {
				// le for débute après la fusion -> on met simplement à jour le numéro Ofs
				autre.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				// dans tous les autres cas, les fors autres impôt ont une validité maximal de 1 jour (= impôt ponctuel) => rien à faire
			}
		}
	}

	protected static class MauvaiseCommuneException extends RuntimeException {
		public MauvaiseCommuneException(String message) {
			super(message);
		}
	}

	void traiteDecisionAci(DecisionAci decision, int nouveauNoOfs, RegDate dateFusion) {
		if (decision.getDateDebut().isAfterOrEqual(dateFusion)) {
			// la décision débute après la fusion -> on met simplement à jour le numéro Ofs
			decision.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
		}
		else {
			Contribuable ctb = decision.getContribuable();
			tiersService.closeDecisionAci(decision, dateFusion.getOneDayBefore());
			tiersService.addDecisionAci(ctb, decision.getTypeAutoriteFiscale(), nouveauNoOfs, dateFusion, null, decision.getRemarque());
		}
	}

	/**
	 * Vérifie que les numéros Ofs spécifiés existent dans le host et qu'Unireg les voit bien ([UNIREG-2056]).
	 *
	 * @param anciensNoOfs les numéros Ofs des anciennes communes
	 * @param nouveauNoOfs le numméro Ofs de la nouvelle commune
	 * @param dateFusion   la date de fusion
	 * @throws MauvaiseCommuneException si l'une des communes n'existe pas, ou si elles ne sont pas toutes dans le même canton
	 */
	private void checkNoOfs(Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion) {
		try {
			final Commune nouvelleCommune = serviceInfra.getCommuneByNumeroOfs(nouveauNoOfs, dateFusion);
			if (nouvelleCommune == null) {
				throw new MauvaiseCommuneException(String.format("La commune avec le numéro OFS %d n'existe pas.", nouveauNoOfs));
			}
			final String nouveauCanton = nouvelleCommune.getSigleCanton();
			if (nouveauCanton == null) {
				throw new MauvaiseCommuneException(String.format("La commune %s (%d) semble n'être rattachée à aucun canton suisse.", nouvelleCommune.getNomOfficiel(), nouvelleCommune.getNoOFS()));
			}

			for (Integer noOfs : anciensNoOfs) {
				final Commune commune = serviceInfra.getCommuneByNumeroOfs(noOfs, dateFusion.getOneDayBefore());
				if (commune == null) {
					throw new MauvaiseCommuneException(String.format("La commune avec le numéro OFS %d n'existe pas.", noOfs));
				}
				final String canton = commune.getSigleCanton();
				if (!nouveauCanton.equals(canton)) {
					throw new MauvaiseCommuneException(String.format("L'ancienne commune %s (%d) est dans le canton %s, alors que la nouvelle commune %s (%d) est dans le canton %s",
															 commune.getNomOfficiel(), noOfs, canton, nouvelleCommune.getNomOfficiel(), nouveauNoOfs, nouveauCanton));
				}
			}
		}
		catch (ServiceInfrastructureException e) {
			throw new RuntimeException(e);
		}
	}

	final private static String queryTiers = // --------------------------------
			"SELECT f.tiers.id                                                  "
					+ "FROM                                                     "
					+ "    ForFiscal AS f                                       "
					+ "WHERE                                                    "
					+ "	   f.annulationDate IS null                             "
					+ "	   AND (f.dateFin IS null OR f.dateFin >= :dateFusion)  "
					+ "	   AND f.typeAutoriteFiscale != 'PAYS_HS'               "
					+ "	   AND f.numeroOfsAutoriteFiscale IN (:nosOfs)          "
					+ "ORDER BY f.tiers.id ASC";

	final private static String queryTiersAvecDecision = // --------------------------------
			"SELECT d.contribuable.id                                           "
					+ "FROM                                                     "
					+ "    DecisionAci AS d                                     "
					+ "WHERE                                                    "
					+ "	   d.annulationDate IS null                             "
					+ "	   AND (d.dateFin IS null OR d.dateFin >= :dateFusion)  "
					+ "	   AND d.typeAutoriteFiscale != 'PAYS_HS'               "
					+ "	   AND d.numeroOfsAutoriteFiscale IN (:nosOfs)          "
					+ "ORDER BY d.contribuable.id ASC";

	private List<Long> getListTiersTouchesParFusion(final Set<Integer> anciensNoOfs, final RegDate dateFusion) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final List<Long> list = hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryTiers);
						queryObject.setParameter("dateFusion", dateFusion);
						queryObject.setParameterList("nosOfs", anciensNoOfs);
						//noinspection unchecked
						return queryObject.list();
					}
				});

				return list;
			}
		});
	}

	private List<Long> getListTiersAvecDecisionTouchesParFusion(final Set<Integer> anciensNoOfs, final RegDate dateFusion) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final List<Long> list = hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryTiersAvecDecision);
						queryObject.setParameter("dateFusion", dateFusion);
						queryObject.setParameterList("nosOfs", anciensNoOfs);
						return queryObject.list();
					}
				});

				return list;
			}
		});
	}
}

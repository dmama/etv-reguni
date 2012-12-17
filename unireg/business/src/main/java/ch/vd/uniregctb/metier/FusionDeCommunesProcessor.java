package ch.vd.uniregctb.metier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
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

	private final Logger LOGGER = Logger.getLogger(OuvertureForsContribuablesMajeursProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final ServiceInfrastructureService serviceInfra;
	private final ValidationService validationService;
	private final ValidationInterceptor validationInterceptor;
	private final AdresseService adresseService;

	private final Map<Class<? extends ForFiscal>, Strategy> strategies = new HashMap<Class<? extends ForFiscal>, Strategy>();

	protected FusionDeCommunesResults rapport;

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
	 * for testing purpose only
	 *
	 * @param rapport le rapport qui va bient
	 */
	protected void setRapport(FusionDeCommunesResults rapport) {
		this.rapport = rapport;
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

		final List<Long> list = getListTiersTouchesParFusion(anciensNoOfs, dateFusion);

		// boucle principale sur les contribuables à traiter
		final BatchTransactionTemplate<Long, FusionDeCommunesResults> template =
				new BatchTransactionTemplate<Long, FusionDeCommunesResults>(list, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, s, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, FusionDeCommunesResults>() {

			@Override
			public FusionDeCommunesResults createSubRapport() {
				return new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, FusionDeCommunesResults r) throws Exception {
				rapport = r;
				s.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);
				traiteBatch(batch, anciensNoOfs, nouveauNoOfs, dateFusion, s);
				return !s.interrupted();
			}
		});

		if (status.interrupted()) {
			status.setMessage("Le traitement de la fusion des communes a été interrompu."
					+ " Nombre de contribuables traités au moment de l'interruption = " + rapportFinal.tiersTraites.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("Le traitement de la fusion des communes est terminé." + " Nombre de contribuables traités = "
					+ rapportFinal.tiersTraites.size() + ". Nombre d'erreurs = " + rapportFinal.tiersEnErrors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<Long> batch, final Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, StatusManager s) {
		for (Long id : batch) {
			traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);
			if (s.interrupted()) {
				break;
			}
		}
	}

	protected void traiteTiers(Long id, Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion) {

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
				rapport.tiersTraites.add(id);
			}
			else if (forIgnore) {
				rapport.addTiersIgnoreDejaSurCommuneResultante(tiers);
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
				tiersService.closeForDebiteurPrestationImposable(debiteur, deb, dateFusion.getOneDayBefore(), false);
				tiersService.openForDebiteurPrestationImposable(debiteur, dateFusion, nouveauNoOfs, deb.getTypeAutoriteFiscale());
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
						queryObject.setParameter("dateFusion", dateFusion.index());
						queryObject.setParameterList("nosOfs", anciensNoOfs);
						//noinspection unchecked
						return queryObject.list();
					}
				});

				return list;
			}
		});
	}
}

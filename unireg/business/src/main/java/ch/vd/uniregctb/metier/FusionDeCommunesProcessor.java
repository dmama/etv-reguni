package ch.vd.uniregctb.metier;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	private final Map<Class<? extends ForFiscal>, Strategy> strategies = new HashMap<Class<? extends ForFiscal>, Strategy>();

	protected FusionDeCommunesResults rapport;

	public FusionDeCommunesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;

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
	 * @param status         un status manager  @return les résultats détaillés du traitement
	 * @return les résultats détaillés du traitement
	 */
	public FusionDeCommunesResults run(final Set<Integer> anciensNoOfs, final int nouveauNoOfs, final RegDate dateFusion, final RegDate dateTraitement, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		final FusionDeCommunesResults rapportFinal = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);

		final List<Long> list = getListTiersTouchesParFusion(anciensNoOfs, dateFusion);

		// boucle principale sur les contribuables à traiter
		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(list, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, s, hibernateTemplate);
		template.execute(new BatchTransactionTemplate.BatchCallback<Long>() {

			private List<Long> batchCourant;

			@Override
			public void beforeTransaction() {
				rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
				batchCourant = batch;
				s.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);
				traiteBatch(batch, anciensNoOfs, nouveauNoOfs, dateFusion, s);
				return !s.interrupted();
			}

			@Override
			public void afterTransactionCommit() {
				rapportFinal.add(rapport);
				rapport = null;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (willRetry) {
					// le batch va être rejoué -> on peut ignorer le rapport
					rapport = null;
				}
				else {
					// on ajoute l'exception au rapport final
					Assert.isTrue(1 == batchCourant.size());
					final Long id = batchCourant.get(0);
					rapportFinal.addOnCommitException(id, e);
					rapport = null;
				}
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

		final Tiers tiers = (Tiers) hibernateTemplate.get(Tiers.class, id);
		Assert.notNull(tiers);

		rapport.nbTiersTotal++;

		// On ne traite pas les tiers qui ne valident pas
		final ValidationResults results = tiers.validate();
		if (results.hasErrors()) {
			rapport.addTiersInvalide(tiers, results);
			return;
		}

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

		if (forTraite) {
			rapport.tiersTraites.add(id);
		}
		else if (forIgnore) {
			rapport.addTiersIgnoreDejaSurCommuneResultante(tiers);
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
						principal.getTypeAutoriteFiscale(), principal.getModeImposition(), MotifFor.FUSION_COMMUNES, true);
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
				tiersService.openForFiscalSecondaire(contribuable, secondaire.getGenreImpot(), dateFusion, dateFinExistante, secondaire.getMotifRattachement(),
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
				tiersService.openForFiscalAutreElementImposable(contribuable, autre.getGenreImpot(), dateFusion, autre.getMotifRattachement(), nouveauNoOfs, autre.getTypeAutoriteFiscale(),
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
				tiersService.closeForDebiteurPrestationImposable(debiteur, deb, dateFusion.getOneDayBefore());
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
				final RegDate dateFinExistante = autre.getDateFin();
				tiersService.closeForAutreImpot(autre, dateFusion.getOneDayBefore());
				tiersService.openForAutreImpot(autre.getTiers(), dateFusion, dateFinExistante, nouveauNoOfs, autre.getTypeAutoriteFiscale(), autre.getGenreImpot());
			}
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

	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	private List<Long> getListTiersTouchesParFusion(final Set<Integer> anciensNoOfs, final RegDate dateFusion) {

		final List<Long> list = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryTiers);
				queryObject.setParameter("dateFusion", dateFusion.index());
				queryObject.setParameterList("nosOfs", anciensNoOfs);
				return queryObject.list();
			}
		});

		return list;
	}
}

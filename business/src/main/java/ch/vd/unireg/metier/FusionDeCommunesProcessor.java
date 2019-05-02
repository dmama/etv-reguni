package ch.vd.unireg.metier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.validation.ValidationException;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCommune;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;
import ch.vd.unireg.tiers.ForFiscalAutreImpot;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.LocalizedDateRange;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.ValidationInterceptor;

/**
 * Processor qui effectue les changements sur les fors fiscaux suite à une fusion de communes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class FusionDeCommunesProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(OuvertureForsContribuablesMajeursProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final ServiceInfrastructureService serviceInfra;
	private final ValidationService validationService;
	private final ValidationInterceptor validationInterceptor;
	private final AdresseService adresseService;

	private final Map<Class<? extends LocalizedDateRange>, Strategy<? extends LocalizedDateRange>> strategies = new HashMap<>();

	/**
	 * Externalisé dans une méthode à part pour une problématique de typage fort -> comme ça, on ne peut pas se tromper et mettre n'importe quoi dans la map...
	 * @param clazz la classe de l'élément sur lequel porte la stratégie
	 * @param strategy la stratégie de traitement de la fusion
	 * @param <T> le type de l'élément analysé
	 */
	private <T extends LocalizedDateRange> void addStrategyMapping(Class<T> clazz, Strategy<T> strategy) {
		strategies.put(clazz, strategy);
	}

	public FusionDeCommunesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService, ServiceInfrastructureService serviceInfra,
	                                 ValidationService validationService, ValidationInterceptor validationInterceptor, AdresseService adresseService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.serviceInfra = serviceInfra;
		this.validationService = validationService;
		this.validationInterceptor = validationInterceptor;
		this.adresseService = adresseService;

		addStrategyMapping(ForFiscalPrincipalPP.class, new ForPrincipalPPStrategy());
		addStrategyMapping(ForFiscalPrincipalPM.class, new ForPrincipalPMStrategy());
		addStrategyMapping(ForFiscalSecondaire.class, new ForSecondaireStrategy());
		addStrategyMapping(ForFiscalAutreElementImposable.class, new ForAutreElementImposableStrategy());
		addStrategyMapping(ForDebiteurPrestationImposable.class, new ForDebiteurStrategy());
		addStrategyMapping(ForFiscalAutreImpot.class, new ForAutreImpotStrategy());
		addStrategyMapping(DecisionAci.class, new DecisionAciStrategy());
		addStrategyMapping(DomicileEtablissement.class, new DomicileEtablissementStrategy());
		addStrategyMapping(AllegementFiscalWrapper.class, new AllegementFiscalStrategy());
	}

	protected static final class TiersATraiter {
		final long idTiers;
		boolean concerneParFors;
		boolean concerneParDecisions;
		boolean concerneParDomicilesEtablissement;
		boolean concerneParAllegementsFiscaux;

		public TiersATraiter(long idTiers, boolean concerneParFors, boolean concerneParDecisions, boolean concerneParDomicilesEtablissement, boolean concerneParAllegementsFiscaux) {
			this.idTiers = idTiers;
			this.concerneParFors = concerneParFors;
			this.concerneParDecisions = concerneParDecisions;
			this.concerneParDomicilesEtablissement = concerneParDomicilesEtablissement;
			this.concerneParAllegementsFiscaux = concerneParAllegementsFiscaux;
		}
	}

	private static void composeTiersATraiter(Map<Long, TiersATraiter> destination, Set<Long> idsTiers, boolean fors, boolean decisions, boolean domicilesEtablissement, boolean allegementsFiscaux) {
		if (idsTiers != null && !idsTiers.isEmpty()) {
			for (Long idTiers : idsTiers) {
				final TiersATraiter found = destination.get(idTiers);
				if (found != null) {
					found.concerneParFors |= fors;
					found.concerneParDecisions |= decisions;
					found.concerneParDomicilesEtablissement |= domicilesEtablissement;
					found.concerneParAllegementsFiscaux |= allegementsFiscaux;
				}
				else {
					destination.put(idTiers, new TiersATraiter(idTiers, fors, decisions, domicilesEtablissement, allegementsFiscaux));
				}
			}
		}
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

		final Set<Long> tiersPourFors = new HashSet<>(getListTiersAvecForToucheParFusion(anciensNoOfs, dateFusion));
		final Set<Long> tiersPourDecisions = new HashSet<>(getListTiersAvecDecisionToucheParFusion(anciensNoOfs, dateFusion));
		final Set<Long> tiersPourDomicilesEtablissement = new HashSet<>(getListTiersAvecDomicileEtablissementToucheParFusion(anciensNoOfs, dateFusion));
		final Set<Long> tiersPourAllegementsFiscaux = new HashSet<>(getListTiersAvecAllegementsFiscauxTouchesParFusion(anciensNoOfs, dateFusion));
		final Map<Long, TiersATraiter> tousTiersConcernes = new TreeMap<>();
		composeTiersATraiter(tousTiersConcernes, tiersPourFors, true, false, false, false);
		composeTiersATraiter(tousTiersConcernes, tiersPourDecisions, false, true, false, false);
		composeTiersATraiter(tousTiersConcernes, tiersPourDomicilesEtablissement, false, false, true, false);
		composeTiersATraiter(tousTiersConcernes, tiersPourAllegementsFiscaux, false, false, false, true);

		// boucle principale sur les contribuables à traiter
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, FusionDeCommunesResults> template = new BatchTransactionTemplateWithResults<>(tousTiersConcernes.keySet(), BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, FusionDeCommunesResults>() {

			@Override
			public FusionDeCommunesResults createSubRapport() {
				return new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, FusionDeCommunesResults r) throws Exception {
				s.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());

				// on mappe les ID de tiers sur des données plus structurées avant de les envoyer plus loin
				final List<TiersATraiter> batchData = new ArrayList<>(batch.size());
				for (Long id : batch) {
					batchData.add(tousTiersConcernes.get(id));
				}
				traiteBatch(batchData, anciensNoOfs, nouveauNoOfs, dateFusion, s, r);

				return !s.isInterrupted();
			}
		}, progressMonitor);

		if (status.isInterrupted()) {
			status.setMessage("Le traitement de la fusion des communes a été interrompu."
					+ " Nombre de contribuables traités au moment de l'interruption = " + rapportFinal.tiersTraitesPourFors.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("Le traitement de la fusion des communes est terminé." + " Nombre de contribuables traités = "
					+ rapportFinal.tiersTraitesPourFors.size() + ". Nombre d'erreurs = " + rapportFinal.tiersEnErreur.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<TiersATraiter> batch, final Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, StatusManager s, FusionDeCommunesResults r) {
		for (TiersATraiter data : batch) {
			traiteTiers(data, anciensNoOfs, nouveauNoOfs, dateFusion, r);
			if (s.isInterrupted()) {
				break;
			}
		}
	}

	protected void traiteTiers(TiersATraiter data, Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion, FusionDeCommunesResults r) {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, data.idTiers);
		if (tiers == null) {
			throw new IllegalArgumentException();
		}

		// Désactivation de validation automatique par l'intercepteur
		final boolean wasValidationInterceptorEnabled = validationInterceptor.isEnabled();
		validationInterceptor.setEnabled(false);
		try {
			final FusionDeCommunesResults.ResultatTraitement fors = data.concerneParFors ? traiteLocalisationsDatees(tiers.getForsFiscauxSorted(), anciensNoOfs, nouveauNoOfs, dateFusion) : FusionDeCommunesResults.ResultatTraitement.RIEN_A_FAIRE;
			final FusionDeCommunesResults.ResultatTraitement decisions =
					data.concerneParDecisions ? traiteLocalisationsDatees(((Contribuable) tiers).getDecisionsSorted(), anciensNoOfs, nouveauNoOfs, dateFusion) : FusionDeCommunesResults.ResultatTraitement.RIEN_A_FAIRE;
			final FusionDeCommunesResults.ResultatTraitement domiciles =
					data.concerneParDomicilesEtablissement ? traiteLocalisationsDatees(((Etablissement) tiers).getSortedDomiciles(false), anciensNoOfs, nouveauNoOfs, dateFusion) : FusionDeCommunesResults.ResultatTraitement.RIEN_A_FAIRE;
			final FusionDeCommunesResults.ResultatTraitement allegements =
					data.concerneParAllegementsFiscaux ? traiteAllegementsFiscaux(((Entreprise) tiers).getAllegementsFiscaux(), anciensNoOfs, nouveauNoOfs, dateFusion) : FusionDeCommunesResults.ResultatTraitement.RIEN_A_FAIRE;

			// Validation manuelle après le traitement de tous les éléments
			final ValidationResults validationResults = validationService.validate(tiers);
			if (validationResults.hasErrors()) {
				throw new ValidationException(tiers, validationResults);
			}

			r.addResultat(data.idTiers, fors, decisions, domiciles, allegements);
		}
		catch (ValidationException e) {
			// on ne va pas contrôler plus loin si le tiers valide ou pas, on sait déjà qu'il ne valide pas...
			throw e;
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
	 * Traite une collection d'éléments potentiellement concernés par la fusion de communes
	 * @param elements les éléments à regarder
	 * @param anciensNoOfs les numéros OFS des communes qui fusionnent
	 * @param nouveauNoOfs le numéro OFS de la commune après fusion
	 * @param dateFusion la date de fusion
	 * @return un couple de booléens (au moins un élément traité ; au moins un élément déjà sur la bonne commune)
	 */
	@NotNull
	private <T extends LocalizedDateRange & Annulable> FusionDeCommunesResults.ResultatTraitement traiteLocalisationsDatees(Collection<T> elements, Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion) {
		boolean dejaBonneCommune = false;
		boolean traite = false;

		for (T elt : elements) {
			if (elt.isAnnule()) {
				continue;
			}

			// On ne traite que les éléments correspondant aux communes fusionnées
			if (elt.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS || !anciensNoOfs.contains(elt.getNumeroOfsAutoriteFiscale())) {
				continue;
			}

			// On ne traite que les éléments valides après la date de la fusion
			if (elt.getDateFin() != null && elt.getDateFin().isBefore(dateFusion)) {
				continue;
			}

			// On ignore les fors qui sont déjà sur la commune résultant de la fusion
			if (elt.getNumeroOfsAutoriteFiscale() == nouveauNoOfs) {
				dejaBonneCommune = true;
				continue;
			}

			// le cast ici est sans danger, car par construction, la stratégie associée à la classe est du bon type (voir méthode addStrategyMapping plus haut...)
			//noinspection unchecked
			final Strategy<T> strat = (Strategy<T>) strategies.get(elt.getClass());
			if (strat == null) {
				throw new IllegalArgumentException("Type d'élément non-supporté : " + elt.getClass().getSimpleName());
			}

			strat.traite(elt, nouveauNoOfs, dateFusion);
			traite = true;
		}

		return traite ? FusionDeCommunesResults.ResultatTraitement.TRAITE : (dejaBonneCommune ? FusionDeCommunesResults.ResultatTraitement.DEJA_BONNE_COMMUNE : FusionDeCommunesResults.ResultatTraitement.RIEN_A_FAIRE);
	}

	@NotNull
	private FusionDeCommunesResults.ResultatTraitement traiteAllegementsFiscaux(Collection<AllegementFiscal> elements, Set<Integer> anciensNoOfs, int nouveauNoOfs, RegDate dateFusion) {
		final List<AllegementFiscalWrapper> wrappers = new ArrayList<>(elements.size());
		for (AllegementFiscal af : elements) {
			// on ne met dans la collection que les cas où le numéro OFS de commune est renseigné
			if (af instanceof AllegementFiscalCommune && ((AllegementFiscalCommune) af).getNoOfsCommune() != null) {
				wrappers.add(new AllegementFiscalWrapper((AllegementFiscalCommune) af));
			}
		}
		return traiteLocalisationsDatees(wrappers, anciensNoOfs, nouveauNoOfs, dateFusion);
	}

	/**
	 * Stratégie de traitement d'un élément dans le cas d'une fusion de communes.
	 * @param <F> le type concret d'élément.
	 */
	private abstract class Strategy<F extends LocalizedDateRange> {
		abstract void traite(F localisation, int nouveauNoOfs, RegDate dateFusion);
	}

	private class ForPrincipalPPStrategy extends Strategy<ForFiscalPrincipalPP> {
		@Override
		void traite(ForFiscalPrincipalPP principal, int nouveauNoOfs, RegDate dateFusion) {
			if (principal.getDateDebut().isAfterOrEqual(dateFusion)) {
				// le for débute après la fusion -> on met simplement à jour le numéro Ofs
				principal.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				tiersService.closeForFiscalPrincipal(principal, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES);
				tiersService.openForFiscalPrincipal(principal.getTiers(), dateFusion, principal.getMotifRattachement(), nouveauNoOfs, principal.getTypeAutoriteFiscale(), principal.getModeImposition(), MotifFor.FUSION_COMMUNES);
			}
		}
	}

	private class ForPrincipalPMStrategy extends Strategy<ForFiscalPrincipalPM> {
		@Override
		void traite(ForFiscalPrincipalPM principal, int nouveauNoOfs, RegDate dateFusion) {
			if (principal.getDateDebut().isAfterOrEqual(dateFusion)) {
				// le for débute après la fusion -> on met simplement à jour le numéro Ofs
				principal.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				tiersService.closeForFiscalPrincipal(principal, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES);
				tiersService.openForFiscalPrincipal(principal.getTiers(), dateFusion, principal.getMotifRattachement(), nouveauNoOfs, principal.getTypeAutoriteFiscale(), MotifFor.FUSION_COMMUNES, principal.getGenreImpot());
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
				                              nouveauNoOfs, secondaire.getTypeAutoriteFiscale(), MotifFor.FUSION_COMMUNES, motifFermetureExistant, secondaire.getGenreImpot());
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
				tiersService.openForFiscalAutreElementImposable(contribuable, autre.getGenreImpot(), dateFusion, autre.getMotifRattachement(), nouveauNoOfs, MotifFor.FUSION_COMMUNES);
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
				// dans tous les autres cas, les fors autres impôt ont une validité maximale de 1 jour (= impôt ponctuel) => rien à faire
			}
		}
	}

	private class DecisionAciStrategy extends Strategy<DecisionAci> {
		@Override
		void traite(DecisionAci decision, int nouveauNoOfs, RegDate dateFusion) {
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
	}

	private class DomicileEtablissementStrategy extends Strategy<DomicileEtablissement> {
		@Override
		void traite(DomicileEtablissement domicile, int nouveauNoOfs, RegDate dateFusion) {
			if (domicile.getDateDebut().isAfterOrEqual(dateFusion)) {
				// la décision débute après la fusion -> on met simplement à jour le numéro Ofs
				domicile.setNumeroOfsAutoriteFiscale(nouveauNoOfs);
			}
			else {
				final Etablissement etb = domicile.getEtablissement();
				tiersService.closeDomicileEtablissement(domicile, dateFusion.getOneDayBefore());
				tiersService.addDomicileEtablissement(etb, domicile.getTypeAutoriteFiscale(), nouveauNoOfs, dateFusion, null);
			}
		}
	}

	/**
	 * Wrapper autour d'un allègement fiscal communal en implémentant explicitement l'interface {@link LocalizedDateRange}, ce que
	 * l'allègement fiscal lui-même ne peut pas faire (il n'a pas de champ "type d'autorité fiscale")
	 */
	private static final class AllegementFiscalWrapper implements LocalizedDateRange, Annulable {

		private final AllegementFiscalCommune target;

		public AllegementFiscalWrapper(AllegementFiscalCommune target) {
			this.target = target;
		}

		@Override
		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		}

		@Override
		public Integer getNumeroOfsAutoriteFiscale() {
			return target.getNoOfsCommune();
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return target.isValidAt(date);
		}

		@Override
		public RegDate getDateDebut() {
			return target.getDateDebut();
		}

		@Override
		public RegDate getDateFin() {
			return target.getDateFin();
		}

		@Override
		public boolean isAnnule() {
			return target.isAnnule();
		}
	}

	private class AllegementFiscalStrategy extends Strategy<AllegementFiscalWrapper> {
		@Override
		void traite(AllegementFiscalWrapper wrapper, int nouveauNoOfs, RegDate dateFusion) {
			final AllegementFiscalCommune allegement = wrapper.target;
			if (allegement.getDateDebut().isAfterOrEqual(dateFusion)) {
				// l'allègement débute après la fusion -> on met simplement à jour le numéro Ofs
				allegement.setNoOfsCommune(nouveauNoOfs);
			}
			else {
				final Entreprise e = allegement.getEntreprise();
				tiersService.closeAllegementFiscal(allegement, dateFusion.getOneDayBefore());
				tiersService.openAllegementFiscalCommunal(e, allegement.getPourcentageAllegement(), allegement.getTypeImpot(), nouveauNoOfs, dateFusion, allegement.getType());
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

	private static final String queryTiersAvecFors = // --------------------------------
			"SELECT DISTINCT f.tiers.id                                         "
					+ "FROM                                                     "
					+ "    ForFiscal AS f                                       "
					+ "WHERE                                                    "
					+ "	   f.annulationDate IS null                             "
					+ "	   AND (f.dateFin IS null OR f.dateFin >= :dateFusion)  "
					+ "	   AND f.typeAutoriteFiscale != 'PAYS_HS'               "
					+ "	   AND f.numeroOfsAutoriteFiscale IN (:nosOfs)          "
					+ "ORDER BY f.tiers.id ASC";

	private static final String queryTiersAvecDecision = // --------------------------------
			"SELECT DISTINCT d.contribuable.id                                  "
					+ "FROM                                                     "
					+ "    DecisionAci AS d                                     "
					+ "WHERE                                                    "
					+ "	   d.annulationDate IS null                             "
					+ "	   AND (d.dateFin IS null OR d.dateFin >= :dateFusion)  "
					+ "	   AND d.typeAutoriteFiscale != 'PAYS_HS'               "
					+ "	   AND d.numeroOfsAutoriteFiscale IN (:nosOfs)          "
					+ "ORDER BY d.contribuable.id ASC";

	private static final String queryTiersAvecDomicileEtablissement = //---------------------
			"SELECT DISTINCT d.etablissement.id                                 "
					+ "FROM                                                     "
					+ "    DomicileEtablissement AS d                           "
					+ "WHERE                                                    "
					+ "    d.annulationDate IS NULL                             "
					+ "    AND (d.dateFin IS NULL OR d.dateFin >= :dateFusion)  "
					+ "	   AND d.typeAutoriteFiscale != 'PAYS_HS'               "
					+ "    AND d.numeroOfsAutoriteFiscale IN (:nosOfs)          "
					+ "ORDER BY d.etablissement.id ASC";

	private static final String queryTiersAvecAllegementFiscal = //---------------------
			"SELECT DISTINCT d.entreprise.id                                    "
					+ "FROM                                                     "
					+ "    AllegementFiscal AS d                                "
					+ "WHERE                                                    "
					+ "    d.annulationDate IS NULL                             "
					+ "    AND (d.dateFin IS NULL OR d.dateFin >= :dateFusion)  "
					+ "    AND d.noOfsCommune IN (:nosOfs)                      "
					+ "ORDER BY d.entreprise.id ASC";

	private List<Long> getListTiersTouchesParFusion(final Set<Integer> anciensNoOfs, final RegDate dateFusion, final String hqlQuery) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query queryObject = session.createQuery(hqlQuery);
			queryObject.setParameter("dateFusion", dateFusion);
			queryObject.setParameterList("nosOfs", anciensNoOfs);
			//noinspection unchecked
			return (List<Long>) queryObject.list();
		}));
	}

	private List<Long> getListTiersAvecForToucheParFusion(Set<Integer> anciensNoOfs, RegDate dateFusion) {
		return getListTiersTouchesParFusion(anciensNoOfs, dateFusion, queryTiersAvecFors);
	}

	private List<Long> getListTiersAvecDecisionToucheParFusion(Set<Integer> anciensNoOfs, RegDate dateFusion) {
		return getListTiersTouchesParFusion(anciensNoOfs, dateFusion, queryTiersAvecDecision);
	}

	private List<Long> getListTiersAvecDomicileEtablissementToucheParFusion(Set<Integer> anciensNoOfs, RegDate dateFusion) {
		return getListTiersTouchesParFusion(anciensNoOfs, dateFusion, queryTiersAvecDomicileEtablissement);
	}

	private List<Long> getListTiersAvecAllegementsFiscauxTouchesParFusion(Set<Integer> anciensNoOfs, RegDate dateFusion) {
		return getListTiersTouchesParFusion(anciensNoOfs, dateFusion, queryTiersAvecAllegementFiscal);
	}
}

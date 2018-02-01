package ch.vd.unireg.metier.assujettissement;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

/**
 * Implémentation du service de détermination des périodes d'imposition à partir de l'assujettissement d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PeriodeImpositionServiceImpl implements PeriodeImpositionService, InitializingBean {

	private AssujettissementService assujettissementService;
	private ParametreAppService parametreAppService;
	private TiersService tiersService;
	private RegimeFiscalService regimeFiscalService;

	/**
	 * Le calculateur utilisé pour les contribuables PP
	 */
	private PeriodeImpositionPersonnesPhysiquesCalculator ppCalculator;

	/**
	 * Map des calculateurs de période d'imposition disponibles, indexée par la classe (concrète) de contribuable à laquelle ils se rapportent
	 */
	private Map<Class<? extends Contribuable>, PeriodeImpositionCalculator<?>> calculators;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
	}

	/**
	 * Méthode intermédiaire qui sert juste à s'assurer que ce qui est inséré dans la map des {@link #calculators} est correct
	 * (notamment le lien entre la clé et la valeur, pour ce qui est des types)
	 * @param map map à remplir
	 * @param clazz classe (concrete) de contribuable
	 * @param calculator calculateur à associer à cette classe de contribuable
	 * @param <T> type de contribuable
	 */
	private static <T extends Contribuable> void addCalculator(Map<Class<? extends Contribuable>, PeriodeImpositionCalculator<?>> map, Class<T> clazz, PeriodeImpositionCalculator<? super T> calculator) {
		if (Modifier.isAbstract(clazz.getModifiers())) {
			throw new IllegalArgumentException("La classe de contribuable enregistrée ici ne devrait pas être abstraite (" + clazz.getName() + " l'est).");
		}
		if (map.containsKey(clazz)) {
			throw new IllegalArgumentException("La classe de contribuable " + clazz.getName() + " a déjà été associée à un calculateur...");
		}
		map.put(clazz, calculator);
	}

	/**
	 * Construction initiale de la map des calculateurs de période d'imposition
	 * @return la map qui lie les classes de contribuables à leur calculateur attitré
	 */
	private static Map<Class<? extends Contribuable>, PeriodeImpositionCalculator<?>> buildAssujettissementCalculators(PeriodeImpositionCalculator<ContribuableImpositionPersonnesPhysiques> ppCalculator,
	                                                                                                                   PeriodeImpositionCalculator<Entreprise> pmCalculator) {
		final Map<Class<? extends Contribuable>, PeriodeImpositionCalculator<?>> map = new HashMap<>();

		//
		// les personnes physiques et ménages communs sont assujettis selon le régime des personnes physiques
		//

		addCalculator(map, PersonnePhysique.class, ppCalculator);
		addCalculator(map, MenageCommun.class, ppCalculator);

		//
		// les entreprises sont assujetties selon le régime des personnes morales
		//

		addCalculator(map, Entreprise.class, pmCalculator);

		return map;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final PeriodeImpositionCalculator<Entreprise> pmCalculator = new PeriodeImpositionPersonnesMoralesCalculator(parametreAppService, tiersService, regimeFiscalService);
		this.ppCalculator = new PeriodeImpositionPersonnesPhysiquesCalculator(parametreAppService);
		this.calculators = buildAssujettissementCalculators(this.ppCalculator, pmCalculator);
	}

	@Nullable
	private <T extends Contribuable> PeriodeImpositionCalculator<? super T> findCalculator(T ctb) {
		//noinspection unchecked
		return (PeriodeImpositionCalculator<? super T>) calculators.get(ctb.getClass());
	}

	/**
	 * Méthode qui appelle au final le calculateur après avoir calculé l'assujettissement
	 * @param ctb contribuable cible du calcul
	 * @param calculator calculateur à utiliser
	 * @param <T> type de contribuable
	 * @return la liste des périodes d'imposition du contribuable donné (ou null s'il n'y en a pas)
	 * @throws AssujettissementException en cas de souci au moment du calcul d'assujettissement
	 */
	@Nullable
	private <T extends Contribuable> List<PeriodeImposition> determinePeriodesImposition(T ctb, PeriodeImpositionCalculator<? super T> calculator) throws AssujettissementException {

		// d'abord, il faut partir de l'assujettissement
		final List<Assujettissement> assujettissements = assujettissementService.determine(ctb);
		if (assujettissements == null || assujettissements.isEmpty()) {
			// pas d'assujettissement, pas de périodes d'imposition non plus...
			return null;
		}

		if (calculator == null) {
			throw new AssujettissementException("Contribuable de type " + ctb.getClass().getSimpleName() + " avec assujettissement -> non-supporté pour le calcul des périodes d'imposition");
		}

		// ensuite, on appelle le calculateur
		final List<PeriodeImposition> pis = calculator.determine(ctb, assujettissements);
		return pis.isEmpty() ? null : pis;
	}

	@Override
	@Nullable
	public List<PeriodeImposition> determine(Contribuable contribuable) throws AssujettissementException {
		final PeriodeImpositionCalculator<? super Contribuable> calculator = findCalculator(contribuable);
		return determinePeriodesImposition(contribuable, calculator);
	}

	@Override
	@Nullable
	public List<PeriodeImposition> determine(Contribuable contribuable, int periodeFiscale) throws AssujettissementException {
		final PeriodeImpositionCalculator<? super Contribuable> calculator = findCalculator(contribuable);
		final PeriodeImpositionCalculator<? super Contribuable> yearLimiting = PeriodeImpositionHelper.periodeFiscaleLimiting(calculator, periodeFiscale);
		return determinePeriodesImposition(contribuable, yearLimiting);
	}

	@Override
	public List<PeriodeImposition> determine(ContribuableImpositionPersonnesPhysiques contribuable, @Nullable DateRange range) throws AssujettissementException {
		final PeriodeImpositionCalculator<? super ContribuableImpositionPersonnesPhysiques> calculator = findCalculator(contribuable);
		final PeriodeImpositionCalculator<? super ContribuableImpositionPersonnesPhysiques> limiting = PeriodeImpositionHelper.rangeIntersecting(calculator, range);
		return determinePeriodesImposition(contribuable, limiting);
	}

	@Override
	public PeriodeImpositionPersonnesPhysiques determinePeriodeImposition(DecompositionForsAnneeComplete fors, Assujettissement assujettissement) {
		List<Assujettissement> assujettissements = null;
		try {
			assujettissements = assujettissementService.determine(fors.contribuable);
		}
		catch (AssujettissementException e) {
			// dommage, mais cela veut juste dire qu'on n'aura pas accès aux assujettissements autres que celui passé en paramètre...
			assujettissement = null;
		}
		return ppCalculator.determinePeriodeImposition(fors, assujettissement, assujettissements);
	}
}

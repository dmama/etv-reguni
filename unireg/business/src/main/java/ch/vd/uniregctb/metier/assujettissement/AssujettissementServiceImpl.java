package ch.vd.uniregctb.metier.assujettissement;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Implémentation du service de détermination de l'assujettissement d'un contribuable à partir de ses fors fiscaux.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class AssujettissementServiceImpl implements AssujettissementService {

	private ValidationService validationService = null;

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	/**
	 * Map des calculateurs d'assujettissement disponibles, indexée par la classe (concrète) de contribuable à laquelle ils se rapportent
	 */
	private final Map<Class<? extends Contribuable>, AssujettissementCalculator<?>> calculators = buildAssujettissementCalculators();

	/**
	 * Méthode intermédiaire qui sert juste à s'assurer que ce qui est inséré dans la map des {@link #calculators} est correct
	 * (notamment le lien entre la clé et la valeur, pour ce qui est des types)
	 * @param map map à remplir
	 * @param clazz classe (concrete) de contribuable
	 * @param calculator calculateur à associer à cette classe de contribuable
	 * @param <T> type de contribuable
	 */
	private static <T extends Contribuable> void addAssujettissementCalculator(Map<Class<? extends Contribuable>, AssujettissementCalculator<?>> map, Class<T> clazz, AssujettissementCalculator<? super T> calculator) {
		if (Modifier.isAbstract(clazz.getModifiers())) {
			throw new IllegalArgumentException("La classe de contribuable enregistrée ici ne devrait pas être abstraite (" + clazz.getName() + " l'est).");
		}
		if (map.containsKey(clazz)) {
			throw new IllegalArgumentException("La classe de contribuable " + clazz.getName() + " a déjà été associée à un calculateur...");
		}
		map.put(clazz, calculator);
	}

	/**
	 * Construction initiale de la map des calculateurs d'assujettissement
	 * @return la map qui lie les classes de contribuables à leur calculateur d'assujettissement attitré
	 */
	private Map<Class<? extends Contribuable>, AssujettissementCalculator<?>> buildAssujettissementCalculators() {
		final Map<Class<? extends Contribuable>, AssujettissementCalculator<?>> map = new HashMap<>();

		//
		// les personnes physiques et ménages communs sont assujettis selon le régime des personnes physiques
		//

		final AssujettissementPersonnesPhysiquesCalculator ppCalculator = new AssujettissementPersonnesPhysiquesCalculator();
		addAssujettissementCalculator(map, PersonnePhysique.class, ppCalculator);
		addAssujettissementCalculator(map, MenageCommun.class, ppCalculator);

		//
		// les entreprises sont assujetties selon le régime des personnes morales
		//

		final AssujettissementPersonnesMoralesCalculator pmCalculator = new AssujettissementPersonnesMoralesCalculator();
		addAssujettissementCalculator(map, Entreprise.class, pmCalculator);

		return map;
	}

	@Override
	public List<Assujettissement> determine(Contribuable ctb) throws AssujettissementException {
		return determineCalculatorEtAssujettissement(ctb, null);
	}

	/**
	 * Appelle le bon calculateur d'assujettissement pour le contribuable donné
	 * @param ctb le contribuable en question
	 * @param noOfsCommunesVaudoises (optionnelle) la liste des numéros OFS des communes vaudoises pour lesquelles on veut spécifiquement calculer l'assujettissement
	 * @param <T> le type de contribuable
	 * @return les assujettissements du contribuable, ou <code>null</code> s'il n'en a pas du tout
	 * @throws AssujettissementException en cas de problème pendant le calcul
	 */
	private <T extends Contribuable> List<Assujettissement> determineCalculatorEtAssujettissement(T ctb, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final AssujettissementCalculator<? super T> calculator = findCalculator(ctb);
		if (calculator == null) {
			// pas de calculateur -> pas d'assujettissement....
			return null;
		}

		return determineAssujettissement(ctb, calculator, noOfsCommunesVaudoises);
	}

	/**
	 * Appelle le calculateur d'assujettissement fourni pour le contribuable donné, et essaie de valider le tiers en cas d'exception lors du calcul, pour
	 * donner un indice quant à la cause probable de l'impossibilité de calculer l'assujettissement...
	 * @param ctb le contribuable en question
	 * @param calculator le calculateur à utiliser
	 * @param noOfsCommunesVaudoises (optionnelle) la liste des numéros OFS des communes vaudoises pour lesquelles on veut spécifiquement calculer l'assujettissement
	 * @param <T> le type de contribuable
	 * @return les assujettissements du contribuable, ou <code>null</code> s'il n'en a pas du tout
	 * @throws AssujettissementException en cas de problème pendant le calcul
	 */
	private <T extends Contribuable> List<Assujettissement> determineAssujettissement(T ctb,
	                                                                                  @NotNull AssujettissementCalculator<? super T> calculator,
	                                                                                  @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		try {
			return _determine(ctb, calculator, noOfsCommunesVaudoises);
		}
		catch (AssujettissementException e) {
			if (validationService != null && !validationService.isInValidation()) { // on évite les appels récursifs
				final ValidationResults vr = validationService.validate(ctb);
				if (vr.hasErrors()) {
					// si le contribuable ne valide pas, on est un peu plus explicite
					throw new AssujettissementException("Une exception a été levée sur le contribuable n°" + ctb.getNumero() +
							                                    " lors du calcul des assujettissements, mais en fait le contribuable ne valide pas: " + vr.toString(), e);
				}
			}

			// autrement, on propage simplement l'exception
			throw e;
		}
	}

	@Nullable
	private <T extends Contribuable> AssujettissementCalculator<? super T> findCalculator(T ctb) {
		//noinspection unchecked
		return (AssujettissementCalculator<? super T>) calculators.get(ctb.getClass());
	}

	/**
	 * Méthode la plus interne du calcul d'assujettissement, qui appelle le calculateur
	 * @param ctb contribuable sur lequel on veut calculer un assujettissement
	 * @param calculator calculateur associé au type du contribuable
	 * @param noOfsCommunesVaudoises (optionnel) ensemble des numéro OFS des communes pour lesquelles on veut spécifiquement calculer l'assujettissement
	 * @param <T> type du contribuable
	 * @return la liste des assujettissements telle que calculée par le calculateur
	 * @throws AssujettissementException en cas de problème lors du calcul
	 */
	@Nullable
	protected static <T extends Contribuable> List<Assujettissement> _determine(@NotNull T ctb,
	                                                                            @NotNull AssujettissementCalculator<? super T> calculator,
	                                                                            @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		if (ctb.isAnnule()) {
			// un contribuable annulé n'est évidemment pas assujetti
			return null;
		}

		final ForsParType fpt = ctb.getForsParType(true);
		if (fpt.isEmpty()) {
			// pas de fors -> pas d'assujettissement...
			return null;
		}

		return calculator.determine(ctb, fpt, noOfsCommunesVaudoises);
	}

	@Override
	public List<Assujettissement> determineRole(ContribuableImpositionPersonnesPhysiques ctb) throws AssujettissementException {

		if (ctb.isAnnule()) {
			// un contribuable annulé n'est évidemment pas assujetti
			return null;
		}

		final ForsParType fpt = ctb.getForsParType(true);
		if (fpt.isEmpty()) {
			// pas de fors -> pas d'assujettissement...
			return null;
		}

		return AssujettissementPersonnesPhysiquesCalculator.determineRole(ctb, fpt, null);
	}

	@Override
	public List<SourcierPur> determineSource(ContribuableImpositionPersonnesPhysiques ctb) throws AssujettissementException {

		if (ctb.isAnnule()) {
			// un contribuable annulé n'est évidemment pas assujetti
			return null;
		}

		final ForsParType fpt = ctb.getForsParType(true);
		if (fpt.isEmpty()) {
			// pas de fors -> pas d'assujettissement...
			return null;
		}

		return AssujettissementPersonnesPhysiquesCalculator.determineSource(ctb, fpt, null);
	}

	@Override
	public List<Assujettissement> determinePourCommunes(Contribuable ctb, Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		return determineCalculatorEtAssujettissement(ctb, noOfsCommunesVaudoises);
	}

	@Override
	public List<Assujettissement> determine(Contribuable contribuable, int annee) throws AssujettissementException {
		final AssujettissementCalculator<? super Contribuable> calculator = findCalculator(contribuable);
		if (calculator == null) {
			// pas de calculateur -> pas d'assujettissement
			return null;
		}

		final AssujettissementCalculator<? super Contribuable> limited = AssujettissementHelper.yearLimiting(calculator, annee);
		return determineAssujettissement(contribuable, limited, null);
	}

	@Override
	public List<Assujettissement> determine(Contribuable contribuable, @Nullable final DateRange range, boolean collate) throws AssujettissementException {
		final AssujettissementCalculator<? super Contribuable> calculator = findCalculator(contribuable);
		if (calculator == null) {
			// pas de calculateur -> pas d'assujettissement
			return null;
		}

		final AssujettissementCalculator<? super Contribuable> limited = AssujettissementHelper.rangeLimiting(calculator, range, collate);
		return determineAssujettissement(contribuable, limited, null);
	}
}

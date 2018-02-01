package ch.vd.unireg.metier.assujettissement;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;

/**
 * Quelques méthodes utiles quand on travaille avec les périodes d'imposition
 */
public abstract class PeriodeImpositionHelper {

	/**
	 * Détermine le code segment à placer sur la déclaration de l'année donnée en fonction des codes utilisés l'année précédente ou encore l'année d'avant
	 * @param contribuable contribuable concerné
	 * @param annee année cible pour l'attribution du code segment
	 * @return le code segment récupéré des années précédentes
	 */
	@Nullable
	public static Integer determineCodeSegment(ContribuableImpositionPersonnesPhysiques contribuable, int annee) {
		Integer codeSegment = getCodeSegment(contribuable, annee - 1);
		if (codeSegment == null) {
			codeSegment = getCodeSegment(contribuable, annee - 2);
		}
		return codeSegment;
	}

	/**
	 * @param contribuable contribuable concerné
	 * @param annee année de référence
	 * @return le code segment présent sur la dernière DI de l'année donnée (<code>null</code> si pas de DI)
	 */
	@Nullable
	private static Integer getCodeSegment(ContribuableImpositionPersonnesPhysiques contribuable, int annee) {
		Integer codeSegment = null;
		final DeclarationImpotOrdinairePP di = getDerniereDeclaration(contribuable, annee);
		if (di != null) {
			codeSegment = di.getCodeSegment();
		}
		return codeSegment;
	}

	/**
	 * @param contribuable contribuable concerné
	 * @param annee année de référence
	 * @return la dernière déclaration d'impôt ordinaire de l'année en question
	 */
	@Nullable
	private static DeclarationImpotOrdinairePP getDerniereDeclaration(ContribuableImpositionPersonnesPhysiques contribuable, int annee) {
		DeclarationImpotOrdinairePP derniereDI = null;
		final List<DeclarationImpotOrdinairePP> declarations = contribuable.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, annee, false);
		if (!declarations.isEmpty()) {
			derniereDI = CollectionsUtils.getLastElement(declarations);
		}
		return derniereDI;
	}

	/**
	 * @param calculator calculateur d'origine
	 * @param periodeFiscale période fiscale limitante
	 * @param <C> type de contribuable concerné
	 * @return un calculateur qui limite les résultats du calculateur d'origine à une période fiscale
	 */
	@Nullable
	public static <C extends Contribuable> PeriodeImpositionCalculator<C> periodeFiscaleLimiting(final PeriodeImpositionCalculator<C> calculator, final int periodeFiscale) {
		if (calculator == null) {
			return null;
		}

		return new PeriodeImpositionCalculator<C>() {
			@NotNull
			@Override
			public List<PeriodeImposition> determine(C contribuable, List<Assujettissement> assujettissements) throws AssujettissementException {
				final List<PeriodeImposition> all = calculator.determine(contribuable, assujettissements);
				final List<PeriodeImposition> limited = new ArrayList<>(all.size());
				for (PeriodeImposition pi : all) {
					if (pi.getPeriodeFiscale() == periodeFiscale) {
						limited.add(pi);
					}
				}
				return limited;
			}
		};
	}

	/**
	 * @param calculator calculateur d'origine
	 * @param range range avec lequel l'intersection des périodes d'imposition globale est testée
	 * @param <C> type de contribuable concerné
	 * @return un calculateur qui limite les résultats du calculateur d'origine à ceux qui intersectent le range donné
	 */
	@Nullable
	public static <C extends Contribuable> PeriodeImpositionCalculator<C> rangeIntersecting(final PeriodeImpositionCalculator<C> calculator, final DateRange range) {
		if (calculator == null) {
			return null;
		}

		// en fait, pas de limitation...
		if (range == null || (range.getDateDebut() == null && range.getDateFin() == null)) {
			return calculator;
		}

		// limitation d'intersection
		return new PeriodeImpositionCalculator<C>() {
			@NotNull
			@Override
			public List<PeriodeImposition> determine(C contribuable, List<Assujettissement> assujettissements) throws AssujettissementException {
				final List<PeriodeImposition> all = calculator.determine(contribuable, assujettissements);
				final List<PeriodeImposition> limited = new ArrayList<>(all.size());
				for (PeriodeImposition pi : all) {
					if (DateRangeHelper.intersect(pi, range)) {
						limited.add(pi);
					}
				}
				return limited;
			}
		};
	}
}

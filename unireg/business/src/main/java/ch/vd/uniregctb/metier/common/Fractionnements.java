package ch.vd.uniregctb.metier.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;

/**
 * Les fractionnements déterminés pour un contribuable donné.
 */
public abstract class Fractionnements<FFP extends ForFiscalPrincipal> implements Iterable<Fraction> {

	private final Map<RegDate, Fraction> map = new HashMap<>();

	/**
	 * Analyse les fors fiscaux principaux et retourne les dates (et motifs) de fractionnement.
	 *
	 * @param principaux une liste de fors fiscaux principaux
	 */
	protected Fractionnements(List<FFP> principaux) {

		// Détermine les assujettissements pour le rattachement de type domicile
		final MovingWindow<FFP> iter = new MovingWindow<>(principaux);
		while (iter.hasNext()) {
			final MovingWindow.Snapshot<FFP> snapshot = iter.next();

			// on détermine les fors principaux qui précèdent et suivent immédiatement
			final ForFiscalPrincipalContext<FFP> forPrincipal = new ForFiscalPrincipalContext<>(snapshot);

			// on détecte une éventuelle date de fractionnement à l'ouverture
			final Fraction fractionOuverture = isFractionOuverture(forPrincipal);
			if (fractionOuverture != null) {
				addFractionOuverture(fractionOuverture);
			}

			// on détecte une éventuelle date de fractionnement à la fermeture
			final Fraction fractionFermeture = isFractionFermeture(forPrincipal);
			if (fractionFermeture != null) {
				addFractionFermeture(fractionFermeture);
			}
		}
	}

	protected abstract Fraction isFractionOuverture(ForFiscalPrincipalContext<FFP> forPrincipal);
	protected abstract Fraction isFractionFermeture(ForFiscalPrincipalContext<FFP> forPrincipal);

	private void addFractionOuverture(Fraction fraction) {

		// on ajoute le fraction sur toutes les dates de sa période d'impact
		final DateRange periode = fraction.getPeriodeImpact();
		for (RegDate d = periode.getDateDebut(); d.isBeforeOrEqual(periode.getDateFin()); d = d.getOneDayAfter()) {
			final Fraction f = map.get(d);
			if (f == null) {
				map.put(d, fraction);
			}
			else if (f.getMotifOuverture() == null) {
				f.setMotifOuverture(fraction.getMotifOuverture());
			}
		}

	}

	private void addFractionFermeture(Fraction fraction) {

		// on ajoute le fraction sur toutes les dates de sa période d'impact
		final DateRange periode = fraction.getPeriodeImpact();
		for (RegDate d = periode.getDateDebut(); d.isBeforeOrEqual(periode.getDateFin()); d = d.getOneDayAfter()) {
			final Fraction f = map.get(d);
			if (f == null) {
				map.put(d, fraction);
			}
			else if (fraction instanceof FractionContrariante) {
				final Fraction replacingFraction = new FractionSimple(fraction.getDate(), f.getMotifOuverture(), fraction.getMotifFermeture());
				for (Map.Entry<RegDate, Fraction> entry : map.entrySet()) {
					if (entry.getValue() == f) {
						if (entry.getKey().isBeforeOrEqual(d)) {
							entry.setValue(replacingFraction);
						}
						else {
							entry.setValue(null);
						}
					}
				}
			}
			else if (f.getMotifFermeture() == null) {
				f.setMotifFermeture(fraction.getMotifFermeture());
			}
		}
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Iterator<Fraction> iterator() {
		final List<Fraction> fractionnements = new ArrayList<>(map.values());
		Collections.sort(fractionnements, Comparator.comparing(Fraction::getDate));
		final List<Fraction> list = Collections.unmodifiableList(fractionnements);
		return list.iterator();
	}

	@Nullable
	public Fraction getAt(RegDate date) {
		return map.get(date);
	}
}

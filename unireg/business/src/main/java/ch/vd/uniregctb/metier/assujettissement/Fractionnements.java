package ch.vd.uniregctb.metier.assujettissement;

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
import ch.vd.uniregctb.common.Triplet;
import ch.vd.uniregctb.common.TripletIterator;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;

/**
 * Les fractionnements déterminés pour un contribuable donné.
 */
public abstract class Fractionnements implements Iterable<Fraction> {

	private final Map<RegDate, Fraction> map = new HashMap<>();

	/**
	 * Analyse les fors fiscaux principaux et retourne les dates (et motifs) de fractionnement.
	 *
	 * @param principaux une liste de fors fiscaux principaux
	 */
	protected Fractionnements(List<ForFiscalPrincipal> principaux) {

		// Détermine les assujettissements pour le rattachement de type domicile
		final TripletIterator<ForFiscalPrincipal> iter = new TripletIterator<>(principaux.iterator());
		while (iter.hasNext()) {
			final Triplet<ForFiscalPrincipal> triplet = iter.next();

			// on détermine les fors principaux qui précèdent et suivent immédiatement
			final ForFiscalPrincipalContext forPrincipal = new ForFiscalPrincipalContext(triplet);

			// on détecte une éventuelle date de fractionnement à l'ouverture
			final Fraction fractionOuverture = isFractionOuverture(forPrincipal);
			if (fractionOuverture != null) {

				if (forPrincipal.next != null && AssujettissementServiceImpl.isArriveeHCApresDepartHSMemeAnnee(forPrincipal.current) && !AssujettissementServiceImpl.roleSourcierPur(
						forPrincipal.current)) {
					// dans ce cas précis, on veut utiliser le motif d'ouverture du for suivant comme motif de fractionnement
					fractionOuverture.setMotifOuverture(forPrincipal.next.getMotifOuverture());
				}

				addFractionOuverture(fractionOuverture);
			}

			// on détecte une éventuelle date de fractionnement à la fermeture
			final Fraction fractionFermeture = isFractionFermeture(forPrincipal);
			if (fractionFermeture != null) {
				addFractionFermeture(fractionFermeture);
			}
		}
	}

	protected abstract Fraction isFractionOuverture(ForFiscalPrincipalContext forPrincipal);
	protected abstract Fraction isFractionFermeture(ForFiscalPrincipalContext forPrincipal);

	private void addFractionOuverture(Fraction fraction) {

		// on ajoute le fraction sur toutes les dates de sa période d'impact
		final DateRange periode = fraction.getPeriodeImpact();
		for (RegDate d = periode.getDateDebut(); d.isBeforeOrEqual(periode.getDateFin()); d = d.getOneDayAfter()) {
			final Fraction f = map.get(d);
			if (f == null) {
				map.put(d, fraction);
			}
			else {
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
			else {
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
		Collections.sort(fractionnements, new Comparator<Fraction>() {
			@Override
			public int compare(Fraction o1, Fraction o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
		final List<Fraction> list = Collections.unmodifiableList(fractionnements);
		return list.iterator();
	}

	@Nullable
	public Fraction getAt(RegDate date) {
		return map.get(date);
	}
}

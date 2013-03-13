package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Triplet;
import ch.vd.uniregctb.common.TripletIterator;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Les fractionnements déterminés pour un contribuable donné.
 */
public abstract class Fractionnements implements Iterable<Fraction> {

	private final Map<RegDate, Fraction> map = new HashMap<RegDate, Fraction>();

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
			if (isFractionOuverture(forPrincipal)) {
				final RegDate fraction = forPrincipal.current.getDateDebut();
				MotifFor motifFraction = forPrincipal.current.getMotifOuverture();

				if (forPrincipal.next != null && AssujettissementServiceImpl.isArriveeHCApresDepartHSMemeAnnee(forPrincipal.current) && !AssujettissementServiceImpl.roleSourcierPur(
						forPrincipal.current)) {
					// dans ce cas précis, on veut utiliser le motif d'ouverture du for suivant comme motif de fractionnement
					motifFraction = forPrincipal.next.getMotifOuverture();
				}

				addFractionOuverture(fraction, motifFraction);
			}

			// on détecte une éventuelle date de fractionnement à la fermeture
			if (isFractionFermeture(forPrincipal)) {
				final RegDate fraction = forPrincipal.current.getDateFin().getOneDayAfter();
				final MotifFor motifFraction = forPrincipal.current.getMotifFermeture();

				addFractionFermeture(fraction, motifFraction);
			}
		}
	}

	protected abstract boolean isFractionOuverture(ForFiscalPrincipalContext forPrincipal);
	protected abstract boolean isFractionFermeture(ForFiscalPrincipalContext forPrincipal);

	private void addFractionOuverture(RegDate date, MotifFor motifFor) {
		Fraction fraction = map.get(date);
		if (fraction == null) {
			fraction = new Fraction(date);
			map.put(date, fraction);
		}
		fraction.setMotifOuverture(motifFor);
	}

	private void addFractionFermeture(RegDate date, MotifFor motifFor) {
		Fraction fraction = map.get(date);
		if (fraction == null) {
			fraction = new Fraction(date);
			map.put(date, fraction);
		}
		fraction.setMotifFermeture(motifFor);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Iterator<Fraction> iterator() {
		final List<Fraction> fractionnements = new ArrayList<Fraction>(map.values());
		Collections.sort(fractionnements, new Comparator<Fraction>() {
			@Override
			public int compare(Fraction o1, Fraction o2) {
				return o1.date.compareTo(o2.date);
			}
		});
		final List<Fraction> list = Collections.unmodifiableList(fractionnements);
		return list.iterator();
	}

	public boolean contains(RegDate date) {
		return map.containsKey(date);
	}

	@Nullable
	public Fraction getAt(RegDate date) {
		return map.get(date);
	}
}

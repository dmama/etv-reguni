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
import ch.vd.uniregctb.type.MotifFor;

/**
 * Les fractionnements déterminés pour un contribuable donné.
 */
public class Fractionnements implements Iterable<Fraction> {

	private final Map<RegDate, Fraction> map = new HashMap<RegDate, Fraction>();

	public void addFractionOuverture(RegDate date, MotifFor motifFor) {
		Fraction fraction = map.get(date);
		if (fraction == null) {
			fraction = new Fraction(date);
			map.put(date, fraction);
		}
		fraction.setMotifOuverture(motifFor);
	}
	public void addFractionFermeture(RegDate date, MotifFor motifFor) {
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

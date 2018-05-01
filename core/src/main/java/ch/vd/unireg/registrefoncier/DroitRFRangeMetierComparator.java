package ch.vd.unireg.registrefoncier;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ch.vd.registre.base.date.DateRangeComparator;

/**
 * Comparateur de droits RF qui utilise les critères suivants pour les ordonner les valeurs :
 * <ul>
 * <li>les dates métier</li>
 * <li>le statut virtuel ou non (droit réel / droit virtuel)</li>
 * <li>le type de droit (droit de propriété / servitude)</li>
 * <li>le nombre d'ayants-droits</li>
 * <li>le nombre d'immeubles</li>
 * </ul>
 */
public class DroitRFRangeMetierComparator implements Comparator<DroitRF> {
	@Override
	public int compare(DroitRF o1, DroitRF o2) {
		int c = DateRangeComparator.compareRanges(o1.getRangeMetier(), o2.getRangeMetier());
		if (c != 0) {
			return c;
		}
		c = compareTypeDroit(o1, o2);
		if (c != 0) {
			return c;
		}
		c = compareList(o1.getAyantDroitList(), o2.getAyantDroitList(), AyantDroitRF::getId);
		if (c != 0) {
			return c;
		}
		c = compareList(o1.getImmeubleList(), o2.getImmeubleList(), ImmeubleRF::getId);
		if (c != 0) {
			return c;
		}
		return c;
	}

	private static final Map<Class<? extends DroitRF>, Integer> statusRanking = new HashMap<>();
	static {
		int rank = 0;
		// ordre d'apparition des droits selon leurs types
		statusRanking.put(DroitProprietePersonnePhysiqueRF.class, rank++);
		statusRanking.put(DroitProprietePersonneMoraleRF.class, rank++);
		statusRanking.put(DroitProprieteCommunauteRF.class, rank++);
		statusRanking.put(DroitProprieteImmeubleRF.class, rank++);
		statusRanking.put(UsufruitRF.class, rank++);
		statusRanking.put(DroitHabitationRF.class, rank++);
		statusRanking.put(DroitProprieteVirtuelRF.class, rank++);
		statusRanking.put(UsufruitVirtuelRF.class, rank++);
		statusRanking.put(DroitVirtuelHeriteRF.class, rank);
	}

	private static int compareTypeDroit(DroitRF o1, DroitRF o2) {
		final Class<? extends DroitRF> class1 = o1.getClass();
		final Class<? extends DroitRF> class2 = o2.getClass();
		if (class1 == class2) {
			// les classes sont identiques
			if (o1 instanceof DroitVirtuelHeriteRF) {
				// pour les droits virtuels hérités, on les trie ensuite en fonction de leurs droits de référence
				return compareTypeDroit(((DroitVirtuelHeriteRF) o1).getReference(), ((DroitVirtuelHeriteRF) o2).getReference());
			}
			else if (o1 instanceof DroitVirtuelTransitifRF) {
				// pour les droits virtuels transitifs, on les trie ensuite en fonction de leurs chemins
				final List<DroitRF> chemin1 = ((DroitVirtuelTransitifRF) o1).getChemin();
				final List<DroitRF> chemin2 = ((DroitVirtuelTransitifRF) o2).getChemin();
				int c = Integer.compare(chemin1.size(), chemin2.size());
				if (c != 0) {
					// le chemin le plus court en premier
					return c;
				}
				for (int i = 0; i < chemin1.size(); i++) {
					final DroitRF c1 = chemin1.get(i);
					final DroitRF c2 = chemin2.get(i);
					c = compareTypeDroit(c1, c2);
					if (c != 0) {
						return c;
					}
				}
				// les chemins sont identiques
				return 0;
			}
			else {
				// pour les autres droits, on les considère identiques au niveau de leurs types
				return 0;
			}
		}
		else {
			// les classes sont différentes, on trie selon les classes
			return Integer.compare(statusRanking.get(class1), statusRanking.get(class2));
		}
	}

	/**
	 * Compare les deux listes selon les critères suivants :
	 * <ol>
	 * <li>leurs tailles</li>
	 * <li>leurs membres un à un</li>
	 * </ol>
	 *
	 * @param left         une liste
	 * @param right        une autre liste
	 * @param keyExtractor la fonction pour extraire la clé de comparaison d'un élément des listes
	 * @param <T>          le type des éléments des listes
	 * @param <U>          le type de la clé de comparaison des éléments des listes
	 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
	private <T, U extends Comparable<? super U>> int compareList(List<? extends T> left, List<? extends T> right, Function<? super T, ? extends U> keyExtractor) {

		// on trie par taille des listes
		int c = Integer.compare(left.size(), right.size());
		if (c != 0) {
			return c;
		}

		// ensuite on compare membre-à-membre
		for (int i = 0; i < left.size(); i++) {
			final U lkey = keyExtractor.apply(left.get(i));
			final U rkey = keyExtractor.apply(right.get(i));
			c = lkey.compareTo(rkey);
			if (c != 0) {
				return c;
			}
		}

		return c;
	}
}

package ch.vd.uniregctb.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

public class ListMergerIteratorTest extends WithoutSpringTest {

	/**
	 * Comparateur naturel des entiers
	 */
	private static final Comparator<Integer> COMPARATOR = new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o1 - o2;
		}
	};

	private static <T> void check(List<T> liste1, List<T> liste2, List<T> resultatAttendu, Comparator<T> comparator) {
		final ListMergerIterator<T> iter = new ListMergerIterator<T>(liste1, liste2, comparator);
		int index = 0;
		while (iter.hasNext()) {
			if (index >= resultatAttendu.size()) {
				final StringBuilder b = new StringBuilder();
				b.append("Eléments en plus des éléments attendus :");

				while (iter.hasNext()) {
					final T enTrop = iter.next();
					b.append(" ").append(enTrop);
				}
				Assert.fail(b.toString());
			}

			final T trouve = iter.next();
			final T attendu = resultatAttendu.get(index);
			Assert.assertEquals("Index " + index, attendu, trouve);

			++ index;
		}

		if (index < resultatAttendu.size()) {
			final StringBuilder b = new StringBuilder();
			b.append("Eléments manquants :");
			for (T missing : resultatAttendu.subList(index, resultatAttendu.size())) {
				b.append(" ").append(missing);
			}
			Assert.fail(b.toString());
		}
	}

	@Test
	public void testUneSeuleCollection() throws Exception {
		final List<Integer> listeNonVide = Arrays.asList(1, 5, 8, 10);
		final List<Integer> listeVide = Collections.emptyList();
		check(listeNonVide, listeVide, listeNonVide, COMPARATOR);
		check(listeVide, listeNonVide, listeNonVide, COMPARATOR);
	}

	@Test
	public void testMergeReel() throws Exception {
		final List<Integer> liste1 = Arrays.asList(1, 5, 8, 10, 12);
		final List<Integer> liste2 = Arrays.asList(2, 3, 7);
		final List<Integer> attendu = Arrays.asList(1, 2, 3, 5, 7, 8, 10, 12);
		check(liste1, liste2, attendu, COMPARATOR);
		check(liste2, liste1, attendu, COMPARATOR);
	}

	@Test
	public void testMergeReelMemeLongueur() throws Exception {
		final List<Integer> liste1 = Arrays.asList(1, 5, 8);
		final List<Integer> liste2 = Arrays.asList(2, 3, 7);
		final List<Integer> attendu = Arrays.asList(1, 2, 3, 5, 7, 8);
		check(liste1, liste2, attendu, COMPARATOR);
		check(liste2, liste1, attendu, COMPARATOR);
	}

	@Test
	public void testMergeReelAvecDoublon() throws Exception {
		final List<Integer> liste1 = Arrays.asList(1, 5, 8, 10, 12);
		final List<Integer> liste2 = Arrays.asList(2, 3, 7, 8);
		final List<Integer> attendu = Arrays.asList(1, 2, 3, 5, 7, 8, 8, 10, 12);
		check(liste1, liste2, attendu, COMPARATOR);
		check(liste2, liste1, attendu, COMPARATOR);
	}

	/**
	 * Classe utilisée ici pour distinguer deux valeurs égales au sens du comparateur fourni
	 */
	private static class Data {
		public final int valeur;
		public final String discriminant;

		private Data(int valeur, String discriminant) {
			this.valeur = valeur;
			this.discriminant = discriminant;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Data) {
				final Data autre = (Data) obj;
				return this.valeur == autre.valeur && this.discriminant.equals(autre.discriminant);
			}
			else {
				return false;
			}
		}

		@Override
		public String toString() {
			return String.format("[Valeur = %d, Discriminant = %s]", valeur, discriminant);
		}
	}

	/**
	 * Comparateur sur les objets Data qui ne considère que l'ordre naturel de leur valeur
	 */
	private static final Comparator<Data> DATA_COMPARATOR = new Comparator<Data>() {
		@Override
		public int compare(Data o1, Data o2) {
			return o1.valeur - o2.valeur;
		}
	};

	@Test
	public void testOrdreDoublons() throws Exception {

		// en cas de doublon entre les deux listes (au sens du comparateur), c'est
		// la première liste qui passe d'abord

		final String constanteListe1 = "Liste 1";
		final String constanteListe2 = "Liste 2";
		final Data data11 = new Data(1, constanteListe1);
		final Data data13 = new Data(3, constanteListe1);
		final Data data14 = new Data(4, constanteListe1);
		final Data data21 = new Data(1, constanteListe2);
		final Data data24 = new Data(4, constanteListe2);
		final List<Data> liste1 = Arrays.asList(data11, data13, data14);
		final List<Data> liste2 = Arrays.asList(data21, data24);
		final List<Data> attendu12 = Arrays.asList(data11, data21, data13, data14, data24);
		final List<Data> attendu21 = Arrays.asList(data21, data11, data13, data24, data14);
		check(liste1, liste2, attendu12, DATA_COMPARATOR);
		check(liste2, liste1, attendu21, DATA_COMPARATOR);
	}
}

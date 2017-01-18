package ch.vd.uniregctb.adresse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;

/**
 * Contient les six lignes utilisées pour l'adressage des déclaration d'impôt.
 */
public class AdresseEnvoi implements Serializable {

	private static final long serialVersionUID = 1565959562688896213L;

	private static final int MAX_LIGNES = 6;
	private static final int MANDATORY = 0;

	private static class Data extends LigneAdresse {

		private static final long serialVersionUID = 4861441051607209404L;

		/**
		 * le degré d'optionalité d'une ligne d'adresse. Plus il est élevé et plus l'adresse risque d'être supprimée en case dépassement du
		 * nombre de lignes autorisé.
		 */
		public final int optionalite;

		public Data(String ligne, int optionalite, boolean wrapping) {
			super(ligne, wrapping);
			this.optionalite = optionalite;
		}
	}

	private final List<Data> input = new ArrayList<>();
	private int maxOptionalite;

	private final LigneAdresse[] lignes = new LigneAdresse[MAX_LIGNES];

	public AdresseEnvoi() {
		maxOptionalite = MANDATORY;
	}

	/**
	 * Calcul les six lignes d'adresses à afficher. Si plus de six lignes ont été ajoutées, les lignes les moins signifiantes sont
	 * supprimées automatiquement.
	 */
	private void computeLines() {

		final List<Data> temp = new ArrayList<>(input);
		int niveau = maxOptionalite;

		if (temp.size() > MAX_LIGNES) {
			/*
			 * Plus de six lignes -> supprime des lignes en commençant par les plus optionnelles
			 *
			 * Note : on supprime *toutes* les lignes correspondant à un certain niveau d'optionalité, et on asserte qu'on obtient
			 * exactement six lignes à la fin. Le but est d'obliger l'appelant à définir une granularité suffisamment fine pour qu'il n'y
			 * ait pas de comportement non-défini.
			 */
			while (temp.size() > MAX_LIGNES && niveau > MANDATORY) {
				for (int i = temp.size() - 1; i >= 0; --i) {
					Data data = temp.get(i);
					if (data.optionalite >= niveau) {
						temp.remove(i);
					}
				}
				--niveau;
			}
			Assert.isTrue(temp.size() == MAX_LIGNES, "Il reste " + temp.size()
					+ " lignes après suppression des lignes optionnelles, alors qu'il devrait en rester exactement 6. "
					+ "Pensez à définir des lignes optionnelles, ou à augmenter la granularité des niveaux d'optionalité.");
		}

		/* copie des lignes existantes */
		for (int i = 0; i < temp.size(); ++i) {
			lignes[i] = temp.get(i);
		}

		/* mise à null des lignes non-existantes */
		for (int i = temp.size(); i < MAX_LIGNES; ++i) {
			lignes[i] = null;
		}
	}

	/**
	 * Ajoute une ligne.
	 * @param line  la ligne à ajouter
	 */
	public void addLine(String line) {
		addLine(line, MANDATORY, false);
	}

	public void addLine(String line, boolean wrapping) {
		addLine(line, MANDATORY, wrapping);
	}

	/**
	 * Ajoute une ligne optionelle. Cette ligne peut être supprimée si plus de six lignes sont ajoutées au total.
	 *
	 * @param line
	 *            la ligne à ajouter
	 * @param optionalite
	 *            le degré d'optionalite de la ligne : plus il est élevé et plus la ligne à de chance d'être supprimée en cas de dépassement
	 *            du nombre de lignes autorisé.
	 */
	public void addLine(String line, int optionalite, boolean wrapping) {
		input.add(new Data(line, optionalite, wrapping));
		if (optionalite > maxOptionalite) {
			maxOptionalite = optionalite;
		}
		computeLines();
	}

	@Nullable
	private static String toTexte(@Nullable LigneAdresse ligne) {
		return Optional.ofNullable(ligne).map(LigneAdresse::getTexte).orElse(null);
	}

	@NotNull
	public String[] getLignes() {
		final String[] textes = new String[lignes.length];
		for (int i = 0 ; i < lignes.length ; ++ i) {
			textes[i] = toTexte(lignes[i]);
		}
		return textes;
	}

	@NotNull
	public LigneAdresse[] getLignesAdresse() {
		return lignes;
	}

	/**
	 * Retourne la valeur de ligne spécifiée par son numéro
	 *
	 * @param no
	 *            le numéro de la ligne [1-6]
	 * @return la valeur de la ligne spécifiée
	 */
	public String getLigne(int no) {
		return toTexte(lignes[no - 1]);
	}

	public String getLigne1() {
		return toTexte(lignes[0]);
	}

	public String getLigne2() {
		return toTexte(lignes[1]);
	}

	public String getLigne3() {
		return toTexte(lignes[2]);
	}

	public String getLigne4() {
		return toTexte(lignes[3]);
	}

	public String getLigne5() {
		return toTexte(lignes[4]);
	}

	public String getLigne6() {
		return toTexte(lignes[5]);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AdresseEnvoi)) return false;

		final AdresseEnvoi that = (AdresseEnvoi) o;
		return Arrays.equals(lignes, that.lignes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(lignes);
	}

	@Override
	public String toString() {
		return "AdresseEnvoi{" + "lignes=" + Arrays.asList(lignes) + '}';
	}
}

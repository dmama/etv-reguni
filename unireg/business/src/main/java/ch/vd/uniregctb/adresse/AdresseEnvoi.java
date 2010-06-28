package ch.vd.uniregctb.adresse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.vd.registre.base.utils.Assert;

/**
 * Contient les six lignes utilisées pour l'adressage des déclaration d'impôt.
 */
public class AdresseEnvoi implements Serializable {

	private static final long serialVersionUID = -1580808921461842994L;

	private final int MAX_LIGNES_CH = 6;
	private final int MAX_LIGNES_HS = 7; // [UNIREG-1974] la poste accepte 7 lignes au maximum pour les adresses étrangères
	private final int MANDATORY = 0;

	private static class Data {

		/**
		 * le contenu string d'une ligne d'adresse.
		 */
		public final String ligne;
		/**
		 * le degré d'optionalité d'une ligne d'adresse. Plus il est élevé et plus l'adresse risque d'être supprimée en case dépassement du nombre de lignes autorisé.
		 */
		public final int optionalite;

		public Data(String ligne, int optionalite) {
			this.ligne = ligne != null ? ligne.trim() : null;
			this.optionalite = optionalite;
		}
	}

	private int maxLignes = MAX_LIGNES_CH;
	private final List<Data> input = new ArrayList<Data>();
	private int maxOptionalite;

	private final String[] lignes = new String[MAX_LIGNES_HS];

	/**
	 * Construit une adresse d'envoi pour une adresse située en Suisse
	 */
	public AdresseEnvoi() {
		maxLignes = MAX_LIGNES_CH;
		maxOptionalite = MANDATORY;
	}

	/**
	 * Calcul les six lignes d'adresses à afficher. Si plus de six lignes ont été ajoutées, les lignes les moins signifiantes sont supprimées automatiquement.
	 */
	private void computeLines() {

		List<Data> temp = new ArrayList<Data>(input);
		int niveau = maxOptionalite;

		if (temp.size() > maxLignes) {
			/*
			 * Plus de six lignes -> supprime des lignes en commençant par les plus optionnelles
			 *
			 * Note : on supprime *toutes* les lignes correspondant à un certain niveau d'optionalité, et on asserte qu'on obtient
			 * exactement six lignes à la fin. Le but est d'obliger l'appelant à définir une granularité suffisamment fine pour qu'il n'y
			 * ait pas de comportement non-défini.
			 */
			while (temp.size() > maxLignes && niveau > MANDATORY) {
				for (int i = temp.size() - 1; i >= 0; --i) {
					Data data = temp.get(i);
					if (data.optionalite >= niveau) {
						temp.remove(i);
					}
				}
				--niveau;
			}
			Assert.isTrue(temp.size() == maxLignes, "Il reste " + temp.size()
					+ " lignes après suppression des lignes optionnelles, alors qu'il devrait en rester exactement " + maxLignes + ". "
					+ "Pensez à définir des lignes optionnelles, ou à augmenter la granularité des niveaux d'optionalité.");
		}

		/* copie des lignes existantes */
		for (int i = 0; i < temp.size(); ++i) {
			lignes[i] = temp.get(i).ligne;
		}

		/* mise à null des lignes non-existantes */
		for (int i = temp.size(); i < MAX_LIGNES_HS; ++i) {
			lignes[i] = null;
		}
	}

	/**
	 * Ajoute une ligne.
	 *
	 * @param line la ligne à ajouter.
	 */
	public void addLine(String line) {
		addLine(line, MANDATORY);
	}

	/**
	 * Ajoute une ligne optionelle. Cette ligne peut être supprimée si plus de six lignes sont ajoutées au total.
	 *
	 * @param line        la ligne à ajouter
	 * @param optionalite le degré d'optionalite de la ligne : plus il est élevé et plus la ligne à de chance d'être supprimée en cas de dépassement du nombre de lignes autorisé.
	 */
	public void addLine(String line, int optionalite) {
		input.add(new Data(line, optionalite));
		if (optionalite > maxOptionalite) {
			maxOptionalite = optionalite;
		}
		computeLines();
	}

	public String[] getLignes() {
		return lignes;
	}

	/**
	 * Retourne la valeur de ligne spécifiée par son numéro
	 *
	 * @param no le numéro de la ligne ([1-6] pour une adresse Suisse; [1-7] pour une adresse étrangère).
	 * @return la valeur de la ligne spécifiée
	 */
	public String getLigne(int no) {
		return lignes[no - 1];
	}

	public String getLigne1() {
		return lignes[0];
	}

	public String getLigne2() {
		return lignes[1];
	}

	public String getLigne3() {
		return lignes[2];
	}

	public String getLigne4() {
		return lignes[3];
	}

	public String getLigne5() {
		return lignes[4];
	}

	public String getLigne6() {
		return lignes[5];
	}

	public String getLigne7() {
		return lignes[6];
	}

	/**
	 * Spécifie si l'adresse est en Suisse (maximum 6 lignes) ou hors-Suisse (maximum 7 lignes).
	 *
	 * @param adresseHorsSuisse <b>vrai</b> si l'adresse est hors-Suisse; <b>faux</b> autrement.
	 */
	public void setHorsSuisse(boolean adresseHorsSuisse) {
		maxLignes = adresseHorsSuisse ? MAX_LIGNES_HS : MAX_LIGNES_CH;
	}

	/**
	 * @return <b>vrai</b> si l'adresse est hors-Suisse (maximum 7 lignes); <b>faux</b> s'il s'agit d'une adresse Suisse (maximum 6 lignes).
	 */
	public boolean isHorsSuisse() {
		return maxLignes == MAX_LIGNES_HS;
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
}

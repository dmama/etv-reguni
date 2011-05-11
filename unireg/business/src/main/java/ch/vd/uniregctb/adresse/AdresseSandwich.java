package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Classe qui stocke les adresses sous formes de couches superposées et qui permet d'en obtenir une vue simple (= fusionnée) ou complexe (= le détail des couches) au choix.
 */
public class AdresseSandwich {

	private final List<Couche> couches = new ArrayList<Couche>();
	private final Set<AdresseCouche> types = new HashSet<AdresseCouche>();

	/**
	 * Cache de l'emballage
	 */
	private List<AdresseGenerique> emballage;

	/**
	 * Classe qui stocke le nom et les adresses d'une couche.
	 */
	public static class Couche {
		private final AdresseCouche type;
		private final List<AdresseGenerique> adresses;
		private final AdresseGenerique.Source sourceSurcharge;
		private final Boolean defaultSurcharge;

		private Couche(AdresseCouche type, List<AdresseGenerique> adresses, AdresseGenerique.Source sourceSurcharge, Boolean isDefault) {
			this.type = type;
			this.adresses = adresses;
			this.sourceSurcharge = sourceSurcharge;
			this.defaultSurcharge = isDefault;
		}

		public AdresseCouche getType() {
			return type;
		}

		public List<AdresseGenerique> getAdresses() {
			return adresses;
		}

		public AdresseGenerique.Source getSourceSurcharge() {
			return sourceSurcharge;
		}

		public Boolean getDefaultSurcharge() {
			return defaultSurcharge;
		}
	}

	/**
	 * Ajoute une couche au sandwich. Toutes les adresses spécifiées appartiendront à la même couche (et elles sont supposées non-chevauchées). Cette méthode ne fait rien si la liste d'adresse est vide.
	 *
	 * @param nom              le nom de la couche.
	 * @param adresses         les adresses qui appartiennent à la couche
	 * @param sourceSurcharge  la source à utiliser lors de la surcharge
	 * @param defaultSurcharge le flag défaut à utiliser lors de la surcharge
	 */
	public void addCouche(AdresseCouche nom, List<AdresseGenerique> adresses, AdresseGenerique.Source sourceSurcharge, Boolean defaultSurcharge) {
		if (types.contains(nom)) {
			throw new IllegalArgumentException("La couche '" + nom + "' existe déjà !");
		}
		if (adresses.isEmpty()) {
			return;
		}
		final Couche couche = new Couche(nom, adresses, sourceSurcharge, defaultSurcharge);
		couches.add(couche);
		types.add(nom);
		emballage = null;
	}

	/**
	 * <pre>
	 * - "Bonjour madame la boulangère, il y a quoi dans votre sandwich, là ?"
	 * - "Hé bien, il y a une tranche de pain, du beurre, de la moutarde, une belle tranche de jambon, deux cornichons, une tomate,
	 *    et pour finir la tranche de pain du dessus."
	 * </pre>
	 *
	 * @return la liste de toutes les couches du sandwich; ou une liste vide si aucune couche n'a été définie.
	 */
	public List<Couche> decortique() {
		return couches;
	}

	/**
	 * <pre>
	 * - "Bonjour madame la boulangère, il est à quoi votre sandwich, là ?"
	 * - "C'est un sandwich au jambon. Regardez, on voit la tranche de jambon qui dépasse."
	 * </pre>
	 *
	 * @return la vue externe (= fusionnée) des adresses du sandwich
	 */
	public List<AdresseGenerique> emballe() {
		if (couches.isEmpty()) {
			return Collections.emptyList();
		}
		if (emballage != null) {
			return emballage;
		}

		this.emballage = new ArrayList<AdresseGenerique>();
		for (Couche couche : couches) {
			// on surcharge les adresses courantes avec les adresses de la nouvelle couche
			emballage = AdresseMixer.override(this.emballage, couche.getAdresses(), couche.getSourceSurcharge(), couche.getDefaultSurcharge());
		}
		return emballage;
	}
}

package ch.vd.uniregctb.adresse;

import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;


/**
 * Contient les sandwichs des adresses fiscales (= spécifiées dans le registre civil, ou surchargées dans le registre fiscal).
 */
public class AdressesFiscalesSandwich {
	public final AdresseSandwich courrier = new AdresseSandwich();
	public final AdresseSandwich representation = new AdresseSandwich();
	public final AdresseSandwich domicile = new AdresseSandwich();
	public final AdresseSandwich poursuite = new AdresseSandwich();

	/**
	 * Adresses du/des tiers <i>représentant</i> le tiers principal dans le cas d'une poursuite (voir spécification "BesoinsContentieux.doc").
	 */
	public final AdresseSandwich poursuiteAutreTiers = new AdresseSandwich();

	/**
	 * @return la vue externe (= fusionnée) des adresses du sandwich
	 * @see {@link AdresseSandwich#emballe()}
	 */
	public AdressesFiscalesHisto emballe() {
		final AdressesFiscalesHisto histo = new AdressesFiscalesHisto();
		histo.courrier = courrier.emballe();
		histo.representation = representation.emballe();
		histo.poursuite = poursuite.emballe();
		histo.domicile = domicile.emballe();
		histo.poursuiteAutreTiers = poursuiteAutreTiers.emballe();
		return histo;
	}

	public void appliqueDefauts(AdresseCouche nomCouche) {
		ajouteCoucheParDefaut(courrier, nomCouche, domicile, representation, poursuite);
		ajouteCoucheParDefaut(domicile, nomCouche, poursuite, courrier, representation);
		ajouteCoucheParDefaut(representation, nomCouche, courrier, domicile, poursuite);
		ajouteCoucheParDefaut(poursuite, nomCouche, domicile, courrier, representation);
	}

	private static void ajouteCoucheParDefaut(AdresseSandwich sandwich, AdresseCouche couche, AdresseSandwich... defaults) {

		if (defaults == null || defaults.length == 0) {
			return;
		}

		final List<AdresseGenerique> emballage = sandwich.emballe();
		if (DateRangeHelper.isFull(emballage)) {
			// il n'y a pas de trou dans le sandwich -> inutile d'essayer d'appliquer des valeurs par défaut
			return;
		}

		@SuppressWarnings({"unchecked"}) final List<AdresseGenerique> array[] = new List[defaults.length];
		for (int i = 0, defaultsLength = defaults.length; i < defaultsLength; i++) {
			array[i] = defaults[i].emballe();
		}

		// on détermine les trous et les adresses qui permettraient de les remplir
		final List<AdresseGenerique> boucheTrous = AdresseMixer.determineBoucheTrous(emballage, array);
		if (boucheTrous == null) {
			return;
		}

		sandwich.addCouche(couche, boucheTrous, null, true);
	}
}

package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.date.RegDate;

/**
 * Permet d'adapter une adresse de type 'autre tiers' à l'interface générique.
 */
public class AdresseAutreTiersAdapter extends BaseAdresseGeneriqueAdapter {

	private final Long id;

	/**
	 * @param adresse        l'adresse de type 'autre tiers' (et qui appartient au tiers courant)
	 * @param adressePointee l'adresse pointée par l'adresse 'autre tiers' (et qui appartient à l'autre tiers)
	 * @param debut          (optionnel) une nouvelle adresse de début
	 * @param fin            (optionnel) une nouvelle adresse de fin
	 * @param source         la source de l'adresse à publier
	 * @param isDefault      vrai si l'adresse représente une adresse par défaut
	 * @param isAnnule       vrai si l'adresse représente une adresse annulée
	 */
	public AdresseAutreTiersAdapter(AdresseAutreTiers adresse, AdresseGenerique adressePointee, RegDate debut, RegDate fin, Source source, Boolean isDefault, Boolean isAnnule) {
		super(adressePointee, debut, fin, source, isDefault, isAnnule);
		this.id = adresse.getId();
	}

	@Override
	public Long getId() {
		return id; // [UNIREG-3206] On veut retourner l'id de l'adresse 'autre tiers' définie sur le tiers courant (et non pas l'id de l'adresse fiscale du tiers pointé).
	}

	@Override
	public boolean isPermanente() {
		// les adresses autre tiers ne peuvent pas être permanentes (à l'heure actuelle)
		return false;
	}
}

package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.date.RegDate;

/*
 * Cette classe permet d'adapter une adresse générique en surchargeant ses dates de début/fin de validité ou sa source.
 */
public class AdresseGeneriqueAdapter extends BaseAdresseGeneriqueAdapter {

	public AdresseGeneriqueAdapter(AdresseGenerique adresse, Source source, Boolean isDefault) {
		super(adresse, source, isDefault);
	}

	public AdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Boolean isDefault) {
		super(adresse, debut, fin, isDefault);
	}

	public AdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Source source, Boolean isDefault) {
		super(adresse, debut, fin, source, isDefault);
	}
}

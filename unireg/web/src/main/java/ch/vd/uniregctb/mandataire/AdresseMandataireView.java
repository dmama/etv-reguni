package ch.vd.uniregctb.mandataire;

import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.type.TypeMandat;

public class AdresseMandataireView extends AdresseView {

	private String nomDestinataire;
	private TypeMandat typeMandat;

	public String getNomDestinataire() {
		return nomDestinataire;
	}

	public void setNomDestinataire(String nomDestinataire) {
		this.nomDestinataire = nomDestinataire;
	}

	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public void setTypeMandat(TypeMandat typeMandat) {
		this.typeMandat = typeMandat;
	}
}

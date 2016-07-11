package ch.vd.uniregctb.mandataire;

import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.type.TypeMandat;

public class AdresseMandataireView extends AdresseView {

	private String nomDestinataire;
	private TypeMandat typeMandat;
	private boolean withCopy;
	private String libelleGenreImpot;

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

	public boolean isWithCopy() {
		return withCopy;
	}

	public void setWithCopy(boolean withCopy) {
		this.withCopy = withCopy;
	}

	public String getLibelleGenreImpot() {
		return libelleGenreImpot;
	}

	public void setLibelleGenreImpot(String libelleGenreImpot) {
		this.libelleGenreImpot = libelleGenreImpot;
	}
}

package ch.vd.uniregctb.json;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.NatureTiers;

@SuppressWarnings({"UnusedDeclaration"})
public class TiersInfoView {
	long numero;
	String nature;
	DateView dateDebutActivite;

	public TiersInfoView(long numero, NatureTiers nature, RegDate dateDebutActivite) {
		this.numero = numero;
		this.nature = nature.name();
		this.dateDebutActivite = DateView.get(dateDebutActivite);
	}

	public long getNumero() {
		return numero;
	}

	public String getNature() {
		return nature;
	}

	public DateView getDateDebutActivite() {
		return dateDebutActivite;
	}
}

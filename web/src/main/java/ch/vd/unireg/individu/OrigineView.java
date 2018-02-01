package ch.vd.unireg.individu;

import java.io.Serializable;

import ch.vd.unireg.tiers.OriginePersonnePhysique;

public class OrigineView implements Serializable {

	private static final long serialVersionUID = 1032220642059942411L;

	private final String nomCommune;
	private final String sigleCanton;

	public OrigineView(OriginePersonnePhysique origine) {
		this.nomCommune = origine.getLibelle();
		this.sigleCanton = origine.getSigleCanton();
	}

	public OrigineView(String nomCommune, String sigleCanton) {
		this.nomCommune = nomCommune;
		this.sigleCanton = sigleCanton;
	}

	public String getNomCommune() {
		return nomCommune;
	}

	public String getSigleCanton() {
		return sigleCanton;
	}

	public String getNomCommuneAvecCanton() {
		final String cantonalPart = String.format("(%s)", sigleCanton);
		if (nomCommune.endsWith(cantonalPart)) {
			return nomCommune;
		}
		else {
			return String.format("%s %s", nomCommune, cantonalPart);
		}
	}
}

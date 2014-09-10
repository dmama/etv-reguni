package ch.vd.uniregctb.individu;

import java.io.Serializable;

public class OrigineView implements Serializable {

	private static final long serialVersionUID = 74727128049378476L;

	private final String nomCommune;
	private final String sigleCanton;

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

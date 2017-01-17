package ch.vd.uniregctb.adresse;

import java.io.Serializable;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public class LignesAdresse implements Serializable {

	private static final long serialVersionUID = -6507365834908463329L;

	private final LigneAdresse[] lignes;

	public LignesAdresse(@NotNull LigneAdresse[] lignes) {
		this.lignes = lignes;
	}

	@NotNull
	public String[] asTexte() {
		final String[] textes = new String[lignes.length];
		for (int i = 0 ; i < lignes.length ; ++i) {
			textes[i] = Optional.ofNullable(lignes[i]).map(LigneAdresse::getTexte).orElse(null);
		}
		return textes;
	}

	@NotNull
	public LigneAdresse[] asLignes() {
		return lignes;
	}
}

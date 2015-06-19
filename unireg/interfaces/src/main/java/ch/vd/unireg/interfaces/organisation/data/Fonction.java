package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

public class Fonction {

	@NotNull
	private final Partie partie;

	private final String texteFonction;
	private final Autorisation autorisation;
	private final String restrictionAutorisation;

	public Fonction(Autorisation autorisation, @NotNull Partie partie, String texteFonction, String restrictionAutorisation) {
		this.autorisation = autorisation;
		this.partie = partie;
		this.texteFonction = texteFonction;
		this.restrictionAutorisation = restrictionAutorisation;
	}

	public Autorisation getAutorisation() {
		return autorisation;
	}

	@NotNull
	public Partie getPartie() {
		return partie;
	}

	public String getRestrictionAutorisation() {
		return restrictionAutorisation;
	}

	public String getTexteFonction() {
		return texteFonction;
	}
}

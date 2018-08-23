package ch.vd.unireg.declaration;

import org.jetbrains.annotations.NotNull;

public class AjoutDelaiDeclarationException extends DeclarationException {

	public enum Raison {
		DECLARATION_ANNULEE,
		MAUVAIS_ETAT_DECLARATION,
		DATE_OBTENTION_INVALIDE,
		DATE_DELAI_INVALIDE
	}

	@NotNull
	private final Raison raison;

	public AjoutDelaiDeclarationException(@NotNull Raison raison, @NotNull String message) {
		super(message);
		this.raison = raison;
	}

	@NotNull
	public Raison getRaison() {
		return raison;
	}
}

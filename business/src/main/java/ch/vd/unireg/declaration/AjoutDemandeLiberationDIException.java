package ch.vd.unireg.declaration;

import org.jetbrains.annotations.NotNull;

public class AjoutDemandeLiberationDIException extends DeclarationException {

	public enum Raison {
		DECLARATION_ANNULEE,
		MAUVAIS_ETAT_DECLARATION,
		LIBERATION_DEJA_EXISTANT,
		DATE_LIBERATION_INVALIDE
	}

	@NotNull
	private final Raison raison;

	public AjoutDemandeLiberationDIException(@NotNull Raison raison, @NotNull String message) {
		super(message);
		this.raison = raison;
	}

	@NotNull
	public Raison getRaison() {
		return raison;
	}
}

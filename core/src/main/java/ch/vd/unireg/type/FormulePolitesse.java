package ch.vd.unireg.type;

import org.jetbrains.annotations.NotNull;

/**
 * Les formules de politesse utilisées pour un tiers spécifique. Ces formules peuvent être valeurs typiques (voir {@link TypeFormulePolitesse}) ou des valeurs spéficiques.
 */
public class FormulePolitesse {

	private final String salutations;
	@NotNull
	private final String formuleAppel;
	@NotNull
	private final TypeFormulePolitesse type;

	public FormulePolitesse(@NotNull TypeFormulePolitesse formulePolitesse) {
		this.salutations = formulePolitesse.salutations();
		this.formuleAppel = formulePolitesse.formuleAppel();
		this.type = formulePolitesse;
	}

	public FormulePolitesse(String salutations, @NotNull String formuleAppel) {
		this.salutations = salutations;
		this.formuleAppel = formuleAppel;
		this.type = TypeFormulePolitesse.CUSTOM;
	}

	public String getSalutations() {
		return salutations;
	}

	@NotNull
	public String getFormuleAppel() {
		return formuleAppel;
	}

	@NotNull
	public TypeFormulePolitesse getType() {
		return type;
	}
}

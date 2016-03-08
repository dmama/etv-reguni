package ch.vd.uniregctb.declaration.ordinaire.common;

import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;

public class ModeleFeuilleDocumentEditique {

	private final String intituleFeuille;
	private final int nombreFeuilles;
	private final int noCADEV;
	private final Integer noFormulaireACI;

	public ModeleFeuilleDocumentEditique(ModeleFeuilleDocument modele, int nombreFeuilles) {
		this.intituleFeuille = modele.getIntituleFeuille();
		this.noCADEV = modele.getNoCADEV();
		this.noFormulaireACI = modele.getNoFormulaireACI();
		this.nombreFeuilles = nombreFeuilles;
	}

	public String getIntituleFeuille() {
		return intituleFeuille;
	}

	public int getNombreFeuilles() {
		return nombreFeuilles;
	}

	public int getNoCADEV() {
		return noCADEV;
	}

	public Integer getNoFormulaireACI() {
		return noFormulaireACI;
	}
}

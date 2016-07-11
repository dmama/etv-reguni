package ch.vd.uniregctb.editique;

import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;

public class ModeleFeuilleDocumentEditique {

	private String intituleFeuille;
	private int nombreFeuilles;
	private int noCADEV;
	private Integer noFormulaireACI;

	public ModeleFeuilleDocumentEditique() {
	}

	public ModeleFeuilleDocumentEditique(ModeleFeuilleDocument modele, int nombreFeuilles) {
		this.intituleFeuille = modele.getIntituleFeuille();
		this.noCADEV = modele.getNoCADEV();
		this.noFormulaireACI = modele.getNoFormulaireACI();
		this.nombreFeuilles = nombreFeuilles;
	}

	public String getIntituleFeuille() {
		return intituleFeuille;
	}

	public void setIntituleFeuille(String intituleFeuille) {
		this.intituleFeuille = intituleFeuille;
	}

	public int getNombreFeuilles() {
		return nombreFeuilles;
	}

	public void setNombreFeuilles(int nombreFeuilles) {
		this.nombreFeuilles = nombreFeuilles;
	}

	public int getNoCADEV() {
		return noCADEV;
	}

	public void setNoCADEV(int noCADEV) {
		this.noCADEV = noCADEV;
	}

	public Integer getNoFormulaireACI() {
		return noFormulaireACI;
	}

	public void setNoFormulaireACI(Integer noFormulaireACI) {
		this.noFormulaireACI = noFormulaireACI;
	}
}

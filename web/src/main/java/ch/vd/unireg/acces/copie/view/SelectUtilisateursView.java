package ch.vd.unireg.acces.copie.view;

import ch.vd.unireg.type.TypeOperation;

public class SelectUtilisateursView {

	private String utilisateurReference;
	private String utilisateurDestination;
	private String visaUtilisateurReference;
	private String visaUtilisateurDestination;
	private TypeOperation typeOperation;

	public String getUtilisateurReference() {
		return utilisateurReference;
	}

	public void setUtilisateurReference(String utilisateurReference) {
		this.utilisateurReference = utilisateurReference;
	}

	public String getUtilisateurDestination() {
		return utilisateurDestination;
	}

	public void setUtilisateurDestination(String utilisateurDestination) {
		this.utilisateurDestination = utilisateurDestination;
	}

	public String getVisaUtilisateurReference() {
		return visaUtilisateurReference;
	}

	public void setVisaUtilisateurReference(String visaUtilisateurReference) {
		this.visaUtilisateurReference = visaUtilisateurReference;
	}

	public String getVisaUtilisateurDestination() {
		return visaUtilisateurDestination;
	}

	public void setVisaUtilisateurDestination(String visaUtilisateurDestination) {
		this.visaUtilisateurDestination = visaUtilisateurDestination;
	}

	public TypeOperation getTypeOperation() {
		return typeOperation;
	}

	public void setTypeOperation(TypeOperation typeOperation) {
		this.typeOperation = typeOperation;
	}
}

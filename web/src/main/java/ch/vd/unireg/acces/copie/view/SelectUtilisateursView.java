package ch.vd.uniregctb.acces.copie.view;

import ch.vd.uniregctb.type.TypeOperation;

public class SelectUtilisateursView {

	private String utilisateurReference;
	private Long numeroUtilisateurReference;
	private String utilisateurDestination;
	private Long numeroUtilisateurDestination;
	private TypeOperation typeOperation;

	public String getUtilisateurReference() {
		return utilisateurReference;
	}
	public void setUtilisateurReference(String utilisateurReference) {
		this.utilisateurReference = utilisateurReference;
	}
	public Long getNumeroUtilisateurReference() {
		return numeroUtilisateurReference;
	}
	public void setNumeroUtilisateurReference(Long numeroUtilisateurReference) {
		this.numeroUtilisateurReference = numeroUtilisateurReference;
	}
	public String getUtilisateurDestination() {
		return utilisateurDestination;
	}
	public void setUtilisateurDestination(String utilisateurDestination) {
		this.utilisateurDestination = utilisateurDestination;
	}
	public Long getNumeroUtilisateurDestination() {
		return numeroUtilisateurDestination;
	}
	public void setNumeroUtilisateurDestination(Long numeroUtilisateurDestination) {
		this.numeroUtilisateurDestination = numeroUtilisateurDestination;
	}
	public TypeOperation getTypeOperation() {
		return typeOperation;
	}
	public void setTypeOperation(TypeOperation typeOperation) {
		this.typeOperation = typeOperation;
	}

}

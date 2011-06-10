package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.securite.model.CollectiviteOperateur;
import ch.vd.securite.model.Operateur;

public class MockOperateur implements Operateur {

	private static final long serialVersionUID = 6881457901203621434L;

	private String nom;
	private String prenom;
	private String email;
	private long individuNoTechnique;
	private String code;

	public MockOperateur() {
	}

	public MockOperateur(String code, long individuNoTechnique) {
		this.code = code;
		this.individuNoTechnique = individuNoTechnique;
	}

	@Override
	public String getNom() {
		return nom != null ? nom :"Nom";
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Override
	public String getPrenom() {
		return prenom != null ? prenom : "Prénom";
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Override
	public String getEmail() {
		return email != null ? email : "prenom.nom@vd.ch";
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public long getIndividuNoTechnique() {
		return individuNoTechnique;
	}

	public void setIndividuNoTechnique(long individuNoTechnique) {
		this.individuNoTechnique = individuNoTechnique;
	}

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public CollectiviteOperateur[] getCollectivites() {
		return null;
	}
}

package ch.vd.unireg.interfaces.service.host;

import java.io.Serializable;

import ch.vd.unireg.wsclient.iam.IamUser;


public class Operateur implements Serializable {

	private String nom;
	private String prenom;
	private String email;
	private long individuNoTechnique;
	private String code;


	public static Operateur get(ch.vd.securite.model.rest.Operateur o) {
		if (o == null) {
			return null;
		}

		final Operateur op = new Operateur();
		op.setNom(o.getNom());
		op.setPrenom(o.getPrenom());
		op.setEmail(o.getEmail());
		op.setIndividuNoTechnique(o.getIndividuNoTechnique());
		op.setCode(o.getCode());


		return op;
	}

	public static Operateur get(IamUser iamUser) {
		if (iamUser == null) {
			return null;
		}

		final Operateur op = new Operateur();
		op.setNom(iamUser.getLastName());
		op.setPrenom(iamUser.getFirstName());
		op.setEmail(iamUser.getEmail());
		op.setCode(iamUser.getIamId());
		return op;
	}


	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Deprecated
	public Long getIndividuNoTechnique() {
		return individuNoTechnique;
	}

	@Deprecated
	public void setIndividuNoTechnique(long individuNoTechnique) {
		this.individuNoTechnique = individuNoTechnique;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}

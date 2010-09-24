package ch.vd.uniregctb.web.xt.handler;

import ch.vd.securite.model.Operateur;
import org.apache.commons.lang.StringEscapeUtils;

public class WrapperUtilisateur {

	private final String visaOperateur;
	private final String nom;
	private final String prenom;
	private final Long individuNoTechnique;

	public WrapperUtilisateur(Operateur utilisateur) {
		this.visaOperateur = utilisateur.getCode();
		this.nom = StringEscapeUtils.escapeXml(utilisateur.getNom());
		this.prenom = StringEscapeUtils.escapeXml(utilisateur.getPrenom());
		this.individuNoTechnique = utilisateur.getIndividuNoTechnique();
	}

	public String getVisaOperateur() {
		return visaOperateur;
	}

	public String getNom() {
		return nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public Long getIndividuNoTechnique() {
		return individuNoTechnique;
	}
}
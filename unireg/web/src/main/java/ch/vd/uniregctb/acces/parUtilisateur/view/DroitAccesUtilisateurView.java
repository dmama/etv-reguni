package ch.vd.uniregctb.acces.parUtilisateur.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DroitAcces;

public class DroitAccesUtilisateurView extends DroitAcces{

	private static final long serialVersionUID = -7536958236536586042L;

	private Long numeroCTB;
	private String prenomNom;
	private String localite;
	private RegDate dateNaissance;
	private boolean lectureSeule;
	public Long getNumeroCTB() {
		return numeroCTB;
	}
	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}
	public String getPrenomNom() {
		return prenomNom;
	}
	public void setPrenomNom(String prenomNom) {
		this.prenomNom = prenomNom;
	}
	public String getLocalite() {
		return localite;
	}
	public void setLocalite(String localite) {
		this.localite = localite;
	}
	public RegDate getDateNaissance() {
		return dateNaissance;
	}
	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}
	public boolean isLectureSeule() {
		return lectureSeule;
	}
	public void setLectureSeule(boolean lectureSeule) {
		this.lectureSeule = lectureSeule;
	}

}

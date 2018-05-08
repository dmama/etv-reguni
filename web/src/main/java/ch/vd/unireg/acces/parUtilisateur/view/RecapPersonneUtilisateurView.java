package ch.vd.unireg.acces.parUtilisateur.view;

import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.general.view.UtilisateurView;
import ch.vd.unireg.type.TypeDroitAcces;

public class RecapPersonneUtilisateurView {

	/*
	 * OUT seulement -> pour construire le rendu
	 */
	private UtilisateurView utilisateur;
	private TiersGeneralView dossier;

	/*
	 * IN-OUT -> pour construire le rendu ET récupérer les informations postées
	 */
	private String visaOperateur;
	private long noDossier;
	private TypeDroitAcces type;
	private boolean lectureSeule;

	public UtilisateurView getUtilisateur() {
		return utilisateur;
	}

	public void setUtilisateur(UtilisateurView utilisateur) {
		this.utilisateur = utilisateur;
	}

	public String getVisaOperateur() {
		return visaOperateur;
	}

	public void setVisaOperateur(String visaOperateur) {
		this.visaOperateur = visaOperateur;
	}

	public TiersGeneralView getDossier() {
		return dossier;
	}

	public void setDossier(TiersGeneralView dossier) {
		this.dossier = dossier;
	}

	public long getNoDossier() {
		return noDossier;
	}

	public void setNoDossier(long noDossier) {
		this.noDossier = noDossier;
	}

	public TypeDroitAcces getType() {
		return type;
	}

	public void setType(TypeDroitAcces type) {
		this.type = type;
	}

	public boolean isLectureSeule() {
		return lectureSeule;
	}

	public void setLectureSeule(boolean lectureSeule) {
		this.lectureSeule = lectureSeule;
	}
}

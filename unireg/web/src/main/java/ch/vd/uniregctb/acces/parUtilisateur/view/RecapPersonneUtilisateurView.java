package ch.vd.uniregctb.acces.parUtilisateur.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class RecapPersonneUtilisateurView {

	private UtilisateurView utilisateur;
	private TiersGeneralView dossier;
	private TypeDroitAcces type;
	private boolean lectureSeule;

	public UtilisateurView getUtilisateur() {
		return utilisateur;
	}
	public void setUtilisateur(UtilisateurView utilisateur) {
		this.utilisateur = utilisateur;
	}
	public TiersGeneralView getDossier() {
		return dossier;
	}
	public void setDossier(TiersGeneralView dossier) {
		this.dossier = dossier;
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

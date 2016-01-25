package ch.vd.uniregctb.acces.parUtilisateur.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class RecapPersonneUtilisateurView {

	/*
	 * OUT seulement -> pour construire le rendu
	 */
	private UtilisateurView utilisateur;
	private TiersGeneralView dossier;

	/*
	 * IN-OUT -> pour construire le rendu ET récupérer les informations postées
	 */
	private long noIndividuOperateur;
	private long noDossier;
	private TypeDroitAcces type;
	private boolean lectureSeule;

	public UtilisateurView getUtilisateur() {
		return utilisateur;
	}

	public void setUtilisateur(UtilisateurView utilisateur) {
		this.utilisateur = utilisateur;
	}

	public long getNoIndividuOperateur() {
		return noIndividuOperateur;
	}

	public void setNoIndividuOperateur(long noIndividuOperateur) {
		this.noIndividuOperateur = noIndividuOperateur;
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

package ch.vd.uniregctb.acces.parUtilisateur.view;

import java.util.List;

import ch.vd.uniregctb.general.view.UtilisateurView;

public class UtilisateurEditRestrictionView {

	private UtilisateurView utilisateur;

	private List<DroitAccesUtilisateurView> restrictions;

	private Integer size;

	public UtilisateurView getUtilisateur() {
		return utilisateur;
	}

	public void setUtilisateur(UtilisateurView utilisateur) {
		this.utilisateur = utilisateur;
	}

	public List<DroitAccesUtilisateurView> getRestrictions() {
		return restrictions;
	}

	public void setRestrictions(List<DroitAccesUtilisateurView> restrictions) {
		this.restrictions = restrictions;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public int getNbDroitsNonAnnules() {
		if (this.restrictions == null) {
			return 0;
		}
		int compteur = 0;
		for (DroitAccesUtilisateurView view : this.restrictions) {
			if (!view.isAnnule()) {
				++ compteur;
			}
		}
		return compteur;
	}
}

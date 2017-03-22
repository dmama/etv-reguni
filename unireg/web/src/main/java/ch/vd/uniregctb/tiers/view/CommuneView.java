package ch.vd.uniregctb.tiers.view;

/**
 * Vue d'une commune qui a un numéro OFS et un nom
 */
public class CommuneView {

	private final int noOfs;
	private final String nom;

	public CommuneView(int noOfs, String nom) {
		this.noOfs = noOfs;
		this.nom = nom;
	}

	public int getNoOfs() {
		return noOfs;
	}

	public String getNom() {
		return nom;
	}
}

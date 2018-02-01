package ch.vd.uniregctb.mouvement.view;

public class BordereauEnvoiView {

	private long id;
	private String nomCollAdmEmettrice;
	private String nomCollAdmDestinataire;
	private int nbMouvementsEnvoyes;
	private int nbMouvementsRecus;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getNomCollAdmEmettrice() {
		return nomCollAdmEmettrice;
	}

	public void setNomCollAdmEmettrice(String nomCollAdmEmettrice) {
		this.nomCollAdmEmettrice = nomCollAdmEmettrice;
	}

	public String getNomCollAdmDestinataire() {
		return nomCollAdmDestinataire;
	}

	public void setNomCollAdmDestinataire(String nomCollAdmDestinataire) {
		this.nomCollAdmDestinataire = nomCollAdmDestinataire;
	}

	public int getNbMouvementsEnvoyes() {
		return nbMouvementsEnvoyes;
	}

	public void setNbMouvementsEnvoyes(int nbMouvementsEnvoyes) {
		this.nbMouvementsEnvoyes = nbMouvementsEnvoyes;
	}

	public int getNbMouvementsRecus() {
		return nbMouvementsRecus;
	}

	public void setNbMouvementsRecus(int nbMouvementsRecus) {
		this.nbMouvementsRecus = nbMouvementsRecus;
	}
}

package ch.vd.uniregctb.mouvement.view;

import java.util.List;

import ch.vd.uniregctb.type.TypeMouvement;

/**
 * Classe de view/command pour le détail des bordereaux à imprimer
 */
public class MouvementMasseDetailBordereauView {

	/**
	 * Type commun à tous les mouvements du bordereau
	 */
	private TypeMouvement typeMouvement;

	/**
	 * Collectivité administrative de départ (envoi) ou de "résidence" (réception pour archives)
	 */
	private String nomCollAdmInitiatrice;

	/**
	 * Collectivité administrative d'arrivée (envoi)
	 */
	private String nomCollAdmDestinataire;

	/**
	 * Détail des mouvements concernés
	 */
	private List<MouvementDetailView> mouvements;

	/**
	 * Identifiants des mouvements sélectionés pour impression effective
	 */
	private long[] selection;

	public List<MouvementDetailView> getMouvements() {
		return mouvements;
	}

	public void setMouvements(List<MouvementDetailView> mouvements) {
		this.mouvements = mouvements;
	}

	public long[] getSelection() {
		return selection;
	}

	public void setSelection(long[] selection) {
		this.selection = selection;
	}

	public TypeMouvement getTypeMouvement() {
		return typeMouvement;
	}

	public void setTypeMouvement(TypeMouvement typeMouvement) {
		this.typeMouvement = typeMouvement;
	}

	public String getNomCollAdmInitiatrice() {
		return nomCollAdmInitiatrice;
	}

	public void setNomCollAdmInitiatrice(String nomCollAdmInitiatrice) {
		this.nomCollAdmInitiatrice = nomCollAdmInitiatrice;
	}

	public String getNomCollAdmDestinataire() {
		return nomCollAdmDestinataire;
	}

	public void setNomCollAdmDestinataire(String nomCollAdmDestinataire) {
		this.nomCollAdmDestinataire = nomCollAdmDestinataire;
	}
}

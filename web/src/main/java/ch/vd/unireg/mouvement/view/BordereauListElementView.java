package ch.vd.uniregctb.mouvement.view;

import ch.vd.uniregctb.type.TypeMouvement;

public class BordereauListElementView {

	private TypeMouvement type;
	private long idCollAdmInitiatrice;
	private int noCollAdmInitiatrice;
	private String nomCollAdmInitiatrice;
	private Long idCollAdmDestinataire;
	private Integer noCollAdmDestinataire;
	private String nomCollAdmDestinataire;
	private int nombreMouvements;

	public TypeMouvement getType() {
		return type;
	}

	public void setType(TypeMouvement type) {
		this.type = type;
	}

	public long getIdCollAdmInitiatrice() {
		return idCollAdmInitiatrice;
	}

	public void setIdCollAdmInitiatrice(long idCollAdmInitiatrice) {
		this.idCollAdmInitiatrice = idCollAdmInitiatrice;
	}

	public int getNoCollAdmInitiatrice() {
		return noCollAdmInitiatrice;
	}

	public void setNoCollAdmInitiatrice(int noCollAdmInitiatrice) {
		this.noCollAdmInitiatrice = noCollAdmInitiatrice;
	}

	public String getNomCollAdmInitiatrice() {
		return nomCollAdmInitiatrice;
	}

	public void setNomCollAdmInitiatrice(String nomCollAdmInitiatrice) {
		this.nomCollAdmInitiatrice = nomCollAdmInitiatrice;
	}

	public Long getIdCollAdmDestinataire() {
		return idCollAdmDestinataire;
	}

	public void setIdCollAdmDestinataire(Long idCollAdmDestinataire) {
		this.idCollAdmDestinataire = idCollAdmDestinataire;
	}

	public Integer getNoCollAdmDestinataire() {
		return noCollAdmDestinataire;
	}

	public void setNoCollAdmDestinataire(Integer noCollAdmDestinataire) {
		this.noCollAdmDestinataire = noCollAdmDestinataire;
	}

	public String getNomCollAdmDestinataire() {
		return nomCollAdmDestinataire;
	}

	public void setNomCollAdmDestinataire(String nomCollAdmDestinataire) {
		this.nomCollAdmDestinataire = nomCollAdmDestinataire;
	}

	public int getNombreMouvements() {
		return nombreMouvements;
	}

	public void setNombreMouvements(int nombreMouvements) {
		this.nombreMouvements = nombreMouvements;
	}
}

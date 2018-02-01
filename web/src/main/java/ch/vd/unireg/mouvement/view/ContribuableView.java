package ch.vd.uniregctb.mouvement.view;

import java.util.List;

public class ContribuableView {

	private Long numero;
	private String nomCommuneGestion;
	private List<String> nomPrenom;

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public String getNomCommuneGestion() {
		return nomCommuneGestion;
	}

	public void setNomCommuneGestion(String nomCommuneGestion) {
		this.nomCommuneGestion = nomCommuneGestion;
	}

	public List<String> getNomPrenom() {
		return nomPrenom;
	}

	public void setNomPrenom(List<String> nomPrenom) {
		this.nomPrenom = nomPrenom;
	}
}

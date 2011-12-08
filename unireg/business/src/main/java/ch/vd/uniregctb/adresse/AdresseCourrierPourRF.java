package ch.vd.uniregctb.adresse;

import java.io.Serializable;

/**
 * TODO Il manque encore la case postale, le pour-adresse, le complément d'adresse... (pas dans la spec)
 */
public class AdresseCourrierPourRF implements Serializable {

	private final String nomPrenom1;
	private final String nomPrenom2;
	private final String rueEtNumero;
	private final String npa;
	private final String localite;
	private final String pays;

	public AdresseCourrierPourRF(String nomPrenom1, String nomPrenom2, String rueEtNumero, String npa, String localite, String pays) {
		this.nomPrenom1 = nomPrenom1;
		this.nomPrenom2 = nomPrenom2;
		this.rueEtNumero = rueEtNumero;
		this.npa = npa;
		this.localite = localite;
		this.pays = pays;
	}

	public String getNomPrenom1() {
		return nomPrenom1;
	}

	public String getNomPrenom2() {
		return nomPrenom2;
	}

	public String getRueEtNumero() {
		return rueEtNumero;
	}

	public String getNpa() {
		return npa;
	}

	public String getLocalite() {
		return localite;
	}

	public String getPays() {
		return pays;
	}
}

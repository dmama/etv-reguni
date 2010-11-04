package ch.vd.uniregctb.general.view;

import ch.vd.registre.base.date.RegDate;

/**
 * TiersGeneralView
 *
 * @author xcifde
 *
 */
public class TiersGeneralView {

	public TiersGeneralView() {
	}

	public TiersGeneralView(Long numero) {
		this.numero = numero;
	}

	private RoleView role;

	private Long numero;

	private RegDate dateNaissance;

	private String numeroAssureSocial;

	private String ancienNumeroAVS;

	private String natureTiers;

	private boolean annule;

	public RoleView getRole() {
		return role;
	}

	public void setRole(RoleView role) {
		this.role = role;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	public String getAncienNumeroAVS() {
		return ancienNumeroAVS;
	}

	public void setAncienNumeroAVS(String ancienNumeroAVS) {
		this.ancienNumeroAVS = ancienNumeroAVS;
	}

	public String getNatureTiers() {
		return natureTiers;
	}

	public void setNatureTiers(String natureTiers) {
		this.natureTiers = natureTiers;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}
}

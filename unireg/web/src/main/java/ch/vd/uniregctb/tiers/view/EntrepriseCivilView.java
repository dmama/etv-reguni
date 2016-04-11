package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EntrepriseCivilView {

	private String sDateCreation;
	private RegDate dateCreation;
	private String raisonSociale;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer numeroOfsSiege;
	private String nomSiege;
	private FormeJuridiqueEntreprise formeJuridique;
	private Long capitalLibere;
	private String devise;
	private String numeroIde;
	private boolean inscriteRC;

	public EntrepriseCivilView() {
		this.typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_HC;
		this.devise = MontantMonetaire.CHF;
	}

	public String getsDateCreation() {
		return sDateCreation;
	}

	public void setsDateCreation(String sDateCreation) {
		this.sDateCreation = sDateCreation;
	}

	public RegDate getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(RegDate dateCreation) {
		this.dateCreation = dateCreation;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	public Integer getNumeroOfsSiege() {
		return numeroOfsSiege;
	}

	public void setNumeroOfsSiege(Integer numeroOfsSiege) {
		this.numeroOfsSiege = numeroOfsSiege;
	}

	public String getNomSiege() {
		return nomSiege;
	}

	public void setNomSiege(String nomSiege) {
		this.nomSiege = nomSiege;
	}

	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	public Long getCapitalLibere() {
		return capitalLibere;
	}

	public void setCapitalLibere(Long capitalLibere) {
		this.capitalLibere = capitalLibere;
	}

	public String getDevise() {
		return devise;
	}

	public void setDevise(String devise) {
		this.devise = devise;
	}

	public String getNumeroIde() {
		return numeroIde;
	}

	public void setNumeroIde(String numeroIde) {
		this.numeroIde = numeroIde;
	}

	public boolean isInscriteRC() {
		return inscriteRC;
	}

	public void setInscriteRC(boolean inscriteRC) {
		this.inscriteRC = inscriteRC;
	}
}

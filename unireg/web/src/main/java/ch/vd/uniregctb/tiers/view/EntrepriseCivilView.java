package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EntrepriseCivilView {

	public enum TypeDefautDate {
		DEFAULT,
		EXPLICT
	}

	private RegDate dateOuverture;
	private String raisonSociale;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer numeroOfsSiege;
	private String nomSiege;
	private FormeJuridiqueEntreprise formeJuridique;

	private TypeDefautDate typeDateDebutExerciceCommercial;
	private RegDate dateDebutExerciceCommercial;

	private TypeDefautDate typeDateFondation;
	private RegDate dateFondation;

	private Long capitalLibere;
	private String devise;
	private String numeroIde;
	private boolean inscriteRC;

	public EntrepriseCivilView() {
		this.typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_HC;
		this.devise = MontantMonetaire.CHF;
		this.typeDateDebutExerciceCommercial = TypeDefautDate.DEFAULT;
		this.typeDateFondation = TypeDefautDate.DEFAULT;
	}

	public RegDate getDateOuverture() {
		return dateOuverture;
	}

	public void setDateOuverture(RegDate dateOuverture) {
		this.dateOuverture = dateOuverture;
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

	public TypeDefautDate getTypeDateDebutExerciceCommercial() {
		return typeDateDebutExerciceCommercial;
	}

	public void setTypeDateDebutExerciceCommercial(TypeDefautDate typeDateDebutExerciceCommercial) {
		this.typeDateDebutExerciceCommercial = typeDateDebutExerciceCommercial;
	}

	public RegDate getDateDebutExerciceCommercial() {
		return dateDebutExerciceCommercial;
	}

	public void setDateDebutExerciceCommercial(RegDate dateDebutExerciceCommercial) {
		this.dateDebutExerciceCommercial = dateDebutExerciceCommercial;
	}

	public TypeDefautDate getTypeDateFondation() {
		return typeDateFondation;
	}

	public void setTypeDateFondation(TypeDefautDate typeDateFondation) {
		this.typeDateFondation = typeDateFondation;
	}

	public RegDate getDateFondation() {
		return dateFondation;
	}

	public void setDateFondation(RegDate dateFondation) {
		this.dateFondation = dateFondation;
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

package ch.vd.uniregctb.evenement.reqdes.reception;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.uniregctb.type.Sexe;

public final class ReqDesPartiePrenante {

	private final int id;
	private NomPrenom nomPrenom;
	private String nomNaissance;
	private Sexe sexe;
	private RegDate dateNaissance;
	private boolean sourceCivile;
	private Long noCtb;
	private String noAvs;
	private NomPrenom nomPrenomMere;
	private NomPrenom nomPrenomPere;
	private RegDate dateDeces;
	private TypeEtatCivil etatCivil;
	private RegDate dateDebutEtatCivil;
	private RegDate dateSeparation;
	private LienPartenaire partner;
	private Adresse adresseResidence;
	private Nationalite nationalite;
	private Permis permis;
	private Origine origine;

	public ReqDesPartiePrenante(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public NomPrenom getNomPrenom() {
		return nomPrenom;
	}

	public void setNomPrenom(NomPrenom nomPrenom) {
		this.nomPrenom = nomPrenom;
	}

	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public boolean isSourceCivile() {
		return sourceCivile;
	}

	public void setSourceCivile(boolean sourceCivile) {
		this.sourceCivile = sourceCivile;
	}

	public Long getNoCtb() {
		return noCtb;
	}

	public void setNoCtb(Long noCtb) {
		this.noCtb = noCtb;
	}

	public String getNoAvs() {
		return noAvs;
	}

	public void setNoAvs(String noAvs) {
		this.noAvs = noAvs;
	}

	public NomPrenom getNomPrenomMere() {
		return nomPrenomMere;
	}

	public void setNomPrenomMere(NomPrenom nomPrenomMere) {
		this.nomPrenomMere = nomPrenomMere;
	}

	public NomPrenom getNomPrenomPere() {
		return nomPrenomPere;
	}

	public void setNomPrenomPere(NomPrenom nomPrenomPere) {
		this.nomPrenomPere = nomPrenomPere;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	public TypeEtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(TypeEtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	public RegDate getDateDebutEtatCivil() {
		return dateDebutEtatCivil;
	}

	public void setDateDebutEtatCivil(RegDate dateDebutEtatCivil) {
		this.dateDebutEtatCivil = dateDebutEtatCivil;
	}

	public RegDate getDateSeparation() {
		return dateSeparation;
	}

	public void setDateSeparation(RegDate dateSeparation) {
		this.dateSeparation = dateSeparation;
	}

	public LienPartenaire getPartner() {
		return partner;
	}

	public void setPartner(LienPartenaire partner) {
		this.partner = partner;
	}

	public Adresse getAdresseResidence() {
		return adresseResidence;
	}

	public void setAdresseResidence(Adresse adresseResidence) {
		this.adresseResidence = adresseResidence;
	}

	public Nationalite getNationalite() {
		return nationalite;
	}

	public void setNationalite(Nationalite nationalite) {
		this.nationalite = nationalite;
	}

	public Permis getPermis() {
		return permis;
	}

	public void setPermis(Permis permis) {
		this.permis = permis;
	}

	public Origine getOrigine() {
		return origine;
	}

	public void setOrigine(Origine origine) {
		this.origine = origine;
	}
}

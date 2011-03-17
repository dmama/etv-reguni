package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class MockAdresse implements Adresse {

	public String casePostale;
	public RegDate dateDebutValidite;
	public RegDate dateFinValidite;
	public String lieu;
	public String localite;
	public String npa;
	public String numero;
	public String numeroAppartement;
	public Integer numeroRue;
	public int numeroOrdrePostal;
	public String numeroPostal;
	public String numeroPostalComplementaire;
	public Integer noOfsPays;
	public String rue;
	public String titre;
	public TypeAdresseCivil typeAdresse;
	public CommuneSimple communeAdresse;

	public MockAdresse() {

	}

	public MockAdresse(String rue, String numero, String numeroPostal, String localite) {
		this.rue = rue;
		this.numero = numero;
		this.numeroPostal = numeroPostal;
		this.localite = localite;
	}

	public MockAdresse(String rue, String numero, String casePostale, String numeroPostal, String localite) {
		this.rue = rue;
		this.numero = numero;
		this.casePostale = casePostale;
		this.numeroPostal = numeroPostal;
		this.localite = localite;
	}

	public String getCasePostale() {
		return casePostale;
	}

	public void setCasePostale(String casePostale) {
		this.casePostale = casePostale;
	}

	public RegDate getDateDebut() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	public RegDate getDateFin() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	public String getLieu() {
		return lieu;
	}

	public void setLieu(String lieu) {
		this.lieu = lieu;
	}

	public String getLocalite() {
		return localite;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	public String getNpa() {
		return npa;
	}

	public void setNpa(String npa) {
		this.npa = npa;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public void setNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
	}

	public Integer getNumeroRue() {
		return numeroRue;
	}

	public void setNumeroRue(Integer numeroRue) {
		this.numeroRue = numeroRue;
	}

	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public void setNumeroOrdrePostal(int numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
	}

	public String getNumeroPostal() {
		return numeroPostal;
	}

	public void setNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
	}

	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	public void setNumeroPostalComplementaire(String numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
	}

	public Integer getNoOfsPays() {
		if (noOfsPays == null) {
			return ServiceInfrastructureService.noOfsSuisse;
		}
		else {
			return noOfsPays;
		}
	}

	public void setNoOfsPays(Integer noOfsPays) {
		this.noOfsPays = noOfsPays;
	}

	public void setPays(Pays pays) {
		this.noOfsPays = pays.getNoOFS();
	}

	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	public String getTitre() {
		return titre;
	}

	public void setTitre(String titre) {
		this.titre = titre;
	}

	public TypeAdresseCivil getTypeAdresse() {
		return typeAdresse;
	}

	public void setTypeAdresse(TypeAdresseCivil typeAdresse) {
		this.typeAdresse = typeAdresse;
	}

	public CommuneSimple getCommuneAdresse() {
		return communeAdresse;
	}

	public void setCommuneAdresse(CommuneSimple c) {
		communeAdresse = c;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebutValidite, dateFinValidite, NullDateBehavior.LATEST);
	}
}

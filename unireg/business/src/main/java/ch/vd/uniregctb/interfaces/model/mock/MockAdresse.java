package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
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
	public Commune communeAdresse;
	public Integer egid;

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

	@Override
	public String getCasePostale() {
		return casePostale;
	}

	public void setCasePostale(String casePostale) {
		this.casePostale = casePostale;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@Override
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

	@Override
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

	@Override
	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	@Override
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public void setNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
	}

	@Override
	public Integer getNumeroRue() {
		return numeroRue;
	}

	public void setNumeroRue(Integer numeroRue) {
		this.numeroRue = numeroRue;
	}

	@Override
	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public void setNumeroOrdrePostal(int numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
	}

	@Override
	public String getNumeroPostal() {
		return numeroPostal;
	}

	public void setNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	public void setNumeroPostalComplementaire(String numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
	}

	@Override
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

	@Override
	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	@Override
	public String getTitre() {
		return titre;
	}

	public void setTitre(String titre) {
		this.titre = titre;
	}

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return typeAdresse;
	}

	public void setTypeAdresse(TypeAdresseCivil typeAdresse) {
		this.typeAdresse = typeAdresse;
	}

	@Override
	public Commune getCommuneAdresse() {
		return communeAdresse;
	}

	public void setCommuneAdresse(Commune c) {
		communeAdresse = c;
	}

	@Override
	public Integer getEgid() {
		return egid;
	}

	public void setEgid(Integer egid) {
		this.egid = egid;
	}

	@Override
	public Integer getEwid() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebutValidite, dateFinValidite, NullDateBehavior.LATEST);
	}
}

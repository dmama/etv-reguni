package ch.vd.unireg.interfaces.infra.mock;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class MockAdresse implements Adresse, Duplicable<MockAdresse> {

	public CasePostale casePostale;
	public RegDate dateDebutValidite;
	public RegDate dateFinValidite;
	public String lieu;
	public String localite;
	public String npa;
	public String numero;
	public String numeroAppartement;
	public Integer numeroRue;
	public Integer numeroOrdrePostal;
	public String numeroPostal;
	public String numeroPostalComplementaire;
	public Integer noOfsPays;
	public String rue;
	public String titre;
	public TypeAdresseCivil typeAdresse;
	public Integer noOfsCommuneAdresse;
	public Integer egid;
	public Integer ewid;
	public Localisation localisationPrecedente;
	public Localisation localisationSuivante;

	public MockAdresse() {
	}

	public MockAdresse(TypeAdresseCivil type, MockRue rue, @Nullable CasePostale casePostale, RegDate debutValidite, @Nullable RegDate finValidite) {
		this(type, casePostale, null, rue.getLocalite(), debutValidite, finValidite);
		this.rue = rue.getDesignationCourrier();
		this.numeroRue = rue.getNoRue();
	}

	public MockAdresse(TypeAdresseCivil type, MockBatiment batiment, @Nullable Integer ewid, CasePostale casePostale, RegDate debutValidite, RegDate finValidite) {
		this(type, batiment.getRue(), casePostale, debutValidite, finValidite);
		this.egid = batiment.getEgid();
		this.ewid = ewid;
	}

	public MockAdresse(TypeAdresseCivil type, String rue, @Nullable CasePostale casePostale, String npaLocalite, MockPays pays, RegDate debutValidite, @Nullable RegDate finValidite) {
		Assert.isFalse(pays.getNoOFS() == ServiceInfrastructureRaw.noOfsSuisse, "Pour la Suisse, il faut utiliser une autre méthode newAdresse");
		this.typeAdresse = type;
		this.casePostale = casePostale;
		this.noOfsPays = pays.getNoOFS();
		this.rue = rue;
		this.lieu = npaLocalite;
		this.dateDebutValidite = debutValidite;
		this.dateFinValidite = finValidite;
	}

	public MockAdresse(TypeAdresseCivil type, CasePostale casePostale, String rue, MockLocalite localite, RegDate debutValidite, RegDate finValidite) {
		this.typeAdresse = type;

		// localité
		this.casePostale = casePostale;
		this.localite = localite.getNomAbrege();
		this.numeroPostal = localite.getNPA().toString();
		final Commune c = localite.getCommuneLocalite();
		this.noOfsCommuneAdresse = (c == null ? null : c.getNoOFS());
		this.noOfsPays = MockPays.Suisse.getNoOFS();
		final Integer complementNPA = localite.getComplementNPA();
		this.numeroPostalComplementaire = (complementNPA == null ? null : complementNPA.toString());
		this.numeroOrdrePostal = localite.getNoOrdre();

		//rue
		this.rue = rue;

		// validité
		this.dateDebutValidite = debutValidite;
		this.dateFinValidite = finValidite;
	}

	public MockAdresse(String rue, String numero, String numeroPostal, String localite) {
		this.rue = rue;
		this.numero = numero;
		this.numeroPostal = numeroPostal;
		this.localite = localite;
	}

	public MockAdresse(String rue, String numero, CasePostale casePostale, String numeroPostal, String localite) {
		this.rue = rue;
		this.numero = numero;
		this.casePostale = casePostale;
		this.numeroPostal = numeroPostal;
		this.localite = localite;
	}

	private MockAdresse(MockAdresse source) {
		casePostale = source.casePostale;
		dateDebutValidite = source.dateDebutValidite;
		dateFinValidite = source.dateFinValidite;
		lieu = source.lieu;
		localite = source.localite;
		npa = source.npa;
		numero = source.numero;
		numeroAppartement = source.numeroAppartement;
		numeroRue = source.numeroRue;
		numeroOrdrePostal = source.numeroOrdrePostal;
		numeroPostal = source.numeroPostal;
		numeroPostalComplementaire = source.numeroPostalComplementaire;
		noOfsPays = source.noOfsPays;
		rue = source.rue;
		titre = source.titre;
		typeAdresse = source.typeAdresse;
		noOfsCommuneAdresse = source.noOfsCommuneAdresse;
		egid = source.egid;
		ewid = source.ewid;
		localisationPrecedente = source.localisationPrecedente;
		localisationSuivante = source.localisationSuivante;
	}

	@Override
	public CasePostale getCasePostale() {
		return casePostale;
	}

	public void setCasePostale(CasePostale casePostale) {
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
	public Integer getNumeroOrdrePostal() {
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
			return ServiceInfrastructureRaw.noOfsSuisse;
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

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return noOfsCommuneAdresse;
	}

	public void setCommuneAdresse(Commune c) {
		noOfsCommuneAdresse = (c == null ? null : c.getNoOFS());
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
		return ewid;
	}

	public void setEwid(Integer ewid) {
		this.ewid = ewid;
	}

	@Override
	public Localisation getLocalisationPrecedente() {
		return localisationPrecedente;
	}

	public void setLocalisationPrecedente(Localisation localisationPrecedente) {
		this.localisationPrecedente = localisationPrecedente;
	}

	@Override
	public Localisation getLocalisationSuivante() {
		return localisationSuivante;
	}

	public void setLocalisationSuivante(Localisation localisationSuivante) {
		this.localisationSuivante = localisationSuivante;
	}

	@Override
	public MockAdresse duplicate() {
		return new MockAdresse(this);
	}
}

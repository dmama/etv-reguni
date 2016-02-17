package ch.vd.unireg.interfaces.organisation.data.builder;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;

public class AdresseLegaleBuilder implements DataBuilder<AdresseLegaleRCEnt> {

	private RegDate dateDebut;
	private RegDate dateFin;
	private String localite;
	private String numeroMaison;
	private String numeroAppartement;
	private Integer numeroOrdrePostal;
	private String numeroPostal;
	private String numeroPostalComplementaire;
	private Integer noOfsPays;
	private String rue;
	private String titre;
	private Integer egid;
	private CasePostale casePostale;

	public AdresseLegaleBuilder() {
	}

	public AdresseLegaleBuilder withDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
		return this;
	}

	public AdresseLegaleBuilder withDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
		return this;
	}

	public AdresseLegaleBuilder withLocalite(String localite) {
		this.localite = localite;
		return this;
	}

	public AdresseLegaleBuilder withNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
		return this;
	}

	public AdresseLegaleBuilder withNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
		return this;
	}

	public AdresseLegaleBuilder withNumeroOrdrePostal(Integer numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
		return this;
	}

	public AdresseLegaleBuilder withNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
		return this;
	}

	public AdresseLegaleBuilder withNumeroPostalComplementaire(String numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
		return this;
	}

	public AdresseLegaleBuilder withNoOfsPays(Integer noOfsPays) {
		this.noOfsPays = noOfsPays;
		return this;
	}

	public AdresseLegaleBuilder withRue(String rue) {
		this.rue = rue;
		return this;
	}

	public AdresseLegaleBuilder withTitre(String titre) {
		this.titre = titre;
		return this;
	}

	public AdresseLegaleBuilder withEgid(Integer egid) {
		this.egid = egid;
		return this;
	}

	public AdresseLegaleBuilder withCasePostale(CasePostale casePostale) {
		this.casePostale = casePostale;
		return this;
	}

	public AdresseLegaleRCEnt build() {
		return new AdresseLegaleRCEnt(dateDebut, dateFin, localite, numeroMaison, numeroAppartement, numeroOrdrePostal, numeroPostal, numeroPostalComplementaire,
		                              noOfsPays, rue, titre, egid, casePostale);
	}
}

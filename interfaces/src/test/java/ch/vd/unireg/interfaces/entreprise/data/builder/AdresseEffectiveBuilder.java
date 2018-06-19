package ch.vd.unireg.interfaces.entreprise.data.builder;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.entreprise.data.AdresseEffectiveRCEnt;

public class AdresseEffectiveBuilder implements DataBuilder<AdresseEffectiveRCEnt> {

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

	public AdresseEffectiveBuilder() {
	}

	public AdresseEffectiveBuilder withDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
		return this;
	}

	public AdresseEffectiveBuilder withDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
		return this;
	}

	public AdresseEffectiveBuilder withLocalite(String localite) {
		this.localite = localite;
		return this;
	}

	public AdresseEffectiveBuilder withNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
		return this;
	}

	public AdresseEffectiveBuilder withNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
		return this;
	}

	public AdresseEffectiveBuilder withNumeroOrdrePostal(Integer numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
		return this;
	}

	public AdresseEffectiveBuilder withNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
		return this;
	}

	public AdresseEffectiveBuilder withNumeroPostalComplementaire(String numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
		return this;
	}

	public AdresseEffectiveBuilder withNoOfsPays(Integer noOfsPays) {
		this.noOfsPays = noOfsPays;
		return this;
	}

	public AdresseEffectiveBuilder withRue(String rue) {
		this.rue = rue;
		return this;
	}

	public AdresseEffectiveBuilder withTitre(String titre) {
		this.titre = titre;
		return this;
	}

	public AdresseEffectiveBuilder withEgid(Integer egid) {
		this.egid = egid;
		return this;
	}

	public AdresseEffectiveBuilder withCasePostale(CasePostale casePostale) {
		this.casePostale = casePostale;
		return this;
	}

	public AdresseEffectiveRCEnt build() {
		return new AdresseEffectiveRCEnt(dateDebut, dateFin, localite, numeroMaison, numeroAppartement, numeroOrdrePostal, numeroPostal, numeroPostalComplementaire,
		                                 noOfsPays, rue, titre, egid, casePostale);
	}
}

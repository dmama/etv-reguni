package ch.vd.unireg.interfaces.organisation.data.builder;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.organisation.data.AdresseRCEnt;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseBuilder implements DataBuilder<AdresseRCEnt> {

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
	private TypeAdresseCivil typeAdresse;
	private Integer egid;
	private CasePostale casePostale;

	public AdresseBuilder() {
	}

	public AdresseBuilder withDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
		return this;
	}

	public AdresseBuilder withDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
		return this;
	}

	public AdresseBuilder withLocalite(String localite) {
		this.localite = localite;
		return this;
	}

	public AdresseBuilder withNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
		return this;
	}

	public AdresseBuilder withNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
		return this;
	}

	public AdresseBuilder withNumeroOrdrePostal(Integer numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
		return this;
	}

	public AdresseBuilder withNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
		return this;
	}

	public AdresseBuilder withNumeroPostalComplementaire(String numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
		return this;
	}

	public AdresseBuilder withNoOfsPays(Integer noOfsPays) {
		this.noOfsPays = noOfsPays;
		return this;
	}

	public AdresseBuilder withRue(String rue) {
		this.rue = rue;
		return this;
	}

	public AdresseBuilder withTitre(String titre) {
		this.titre = titre;
		return this;
	}

	public AdresseBuilder withTypeAdresse(TypeAdresseCivil type) {
		this.typeAdresse = type;
		return this;
	}

	public AdresseBuilder withEgid(Integer egid) {
		this.egid = egid;
		return this;
	}

	public AdresseBuilder withCasePostale(CasePostale casePostale) {
		this.casePostale = casePostale;
		return this;
	}

	public AdresseRCEnt build() {
		return new AdresseRCEnt(dateDebut, dateFin, localite, numeroMaison, numeroAppartement, numeroOrdrePostal, numeroPostal, numeroPostalComplementaire,
		                        noOfsPays, rue, titre, typeAdresse, egid, casePostale);
	}
}

package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.type.TypeAdressePM;

public class MockAdresseEntreprise implements AdresseEntreprise {

	private String complement;
	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private String localite;
	private String numeroMaison;
	private String rue;
	private TypeAdressePM type;
	private Integer numeroTechniqueRue;
	private Pays pays;
	private int numeroOrdrePostal;
	private String numeroPostal;
	private String numeroPostalComplementaire;

	public void setNumeroTechniqueRue(Integer numeroTechniqueRue) {
		this.numeroTechniqueRue = numeroTechniqueRue;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}

	public void setNumeroOrdrePostal(int numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
	}

	public void setNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
	}

	public void setNumeroPostalComplementaire(String numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
	}

	@Override
	public String getComplement() {
		return complement;
	}

	@Override
	public Integer getNumeroTechniqueRue() {
		return numeroTechniqueRue;
	}

	public void setComplement(String complement) {
		this.complement = complement;
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	@Override
	public String getLocaliteAbregeMinuscule() {
		return localite;
	}

	@Override
	public String getLocaliteCompletMinuscule() {
		return localite;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	@Override
	public String getNumeroMaison() {
		return numeroMaison;
	}

	@Override
	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	@Override
	public String getNumeroPostal() {
		return numeroPostal;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	public void setNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
	}

	@Override
	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	@Override
	public TypeAdressePM getType() {
		return type;
	}

	public void setType(TypeAdressePM type) {
		this.type = type;
	}
}

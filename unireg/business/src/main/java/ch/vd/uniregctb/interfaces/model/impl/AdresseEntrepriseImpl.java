package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.type.TypeAdressePM;

public class AdresseEntrepriseImpl implements AdresseEntreprise, Serializable {

	private static final long serialVersionUID = 2830120339685146006L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final PaysImpl pays;
	private final String complement;
	private final Integer numeroTechniqueRue;
	private final String localiteAbregeMinuscule;
	private final String localiteCompletMinuscule;
	private final String numeroMaison;
	private final int numeroOrdrePostal;
	private final String numeroPostal;
	private final String numeroPostalComplementaire;
	private final String rue;
	private final TypeAdressePM type;

	public static AdresseEntrepriseImpl get(ch.vd.registre.pm.model.AdresseEntreprise target) {
		if (target == null) {
			return null;
		}
		return new AdresseEntrepriseImpl(target);
	}

	private AdresseEntrepriseImpl(ch.vd.registre.pm.model.AdresseEntreprise target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.pays = PaysImpl.get(target.getPays());
		this.complement = target.getComplement();
		this.numeroTechniqueRue = target.getNumeroTechniqueRue();
		this.localiteAbregeMinuscule = target.getLocaliteAbregeMinuscule();
		this.localiteCompletMinuscule = target.getLocaliteCompletMinuscule();
		this.numeroMaison = target.getNumeroMaison();
		this.numeroOrdrePostal = target.getNumeroOrdrePostal();
		this.numeroPostal = target.getNumeroPostal();
		this.numeroPostalComplementaire = target.getNumeroPostalComplementaire();
		this.rue = target.getRue();
		this.type = TypeAdressePM.get(target.getType());
	}

	public String getComplement() {
		return complement;
	}

	public Integer getNumeroTechniqueRue() {
		return numeroTechniqueRue;
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getLocaliteAbregeMinuscule() {
		return localiteAbregeMinuscule;
	}

	public String getLocaliteCompletMinuscule() {
		return localiteCompletMinuscule;
	}

	public Pays getPays() {
		return pays;
	}

	public String getNumeroMaison() {
		return numeroMaison;
	}

	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public String getNumeroPostal() {
		return numeroPostal;
	}

	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	public String getRue() {
		return rue;
	}

	public TypeAdressePM getType() {
		return type;
	}

}

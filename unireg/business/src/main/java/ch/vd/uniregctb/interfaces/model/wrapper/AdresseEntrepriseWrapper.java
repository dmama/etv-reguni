package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.Pays;

public class AdresseEntrepriseWrapper implements AdresseEntreprise, Serializable {

	private static final long serialVersionUID = 2830120339685146006L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final PaysWrapper pays;
	private String complement;
	private Integer numeroTechniqueRue;
	private String localiteAbregeMinuscule;
	private String localiteCompletMinuscule;
	private String numeroMaison;
	private int numeroOrdrePostal;
	private String numeroPostal;
	private String numeroPostalComplementaire;
	private String rue;
	private EnumTypeAdresseEntreprise type;

	public static AdresseEntrepriseWrapper get(ch.vd.registre.pm.model.AdresseEntreprise target) {
		if (target == null) {
			return null;
		}
		return new AdresseEntrepriseWrapper(target);
	}

	private AdresseEntrepriseWrapper(ch.vd.registre.pm.model.AdresseEntreprise target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.pays = PaysWrapper.get(target.getPays());
		this.complement = target.getComplement();
		this.numeroTechniqueRue = target.getNumeroTechniqueRue();
		this.localiteAbregeMinuscule = target.getLocaliteAbregeMinuscule();
		this.localiteCompletMinuscule = target.getLocaliteCompletMinuscule();
		this.numeroMaison = target.getNumeroMaison();
		this.numeroOrdrePostal = target.getNumeroOrdrePostal();
		this.numeroPostal = target.getNumeroPostal();
		this.numeroPostalComplementaire = target.getNumeroPostalComplementaire();
		this.rue = target.getRue();
		this.type = target.getType();
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

	public EnumTypeAdresseEntreprise getType() {
		return type;
	}

}

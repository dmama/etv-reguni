package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.Pays;

public class AdresseEntrepriseWrapper implements AdresseEntreprise {

	private final ch.vd.registre.pm.model.AdresseEntreprise target;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final PaysWrapper pays;

	public static AdresseEntrepriseWrapper get(ch.vd.registre.pm.model.AdresseEntreprise target) {
		if (target == null) {
			return null;
		}
		return new AdresseEntrepriseWrapper(target);
	}

	private AdresseEntrepriseWrapper(ch.vd.registre.pm.model.AdresseEntreprise target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.pays = PaysWrapper.get(target.getPays());
	}

	public String getComplement() {
		return target.getComplement();
	}

	public Integer getNumeroTechniqueRue() {
		return target.getNumeroTechniqueRue();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getLocaliteAbregeMinuscule() {
		return target.getLocaliteAbregeMinuscule();
	}

	public String getLocaliteCompletMinuscule() {
		return target.getLocaliteCompletMinuscule();
	}

	public Pays getPays() {
		return pays;
	}

	public String getNumeroMaison() {
		return target.getNumeroMaison();
	}

	public int getNumeroOrdrePostal() {
		return target.getNumeroOrdrePostal();
	}

	public String getNumeroPostal() {
		return target.getNumeroPostal();
	}

	public String getNumeroPostalComplementaire() {
		return target.getNumeroPostalComplementaire();
	}

	public String getRue() {
		return target.getRue();
	}

	public EnumTypeAdresseEntreprise getType() {
		return target.getType();
	}

}

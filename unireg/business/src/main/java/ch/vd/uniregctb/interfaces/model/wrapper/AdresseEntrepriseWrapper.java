package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;

public class AdresseEntrepriseWrapper implements AdresseEntreprise {

	private final ch.vd.registre.pm.model.AdresseEntreprise target;
	private final RegDate dateDebut;
	private final RegDate dateFin;

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
	}

	public String getComplement() {
		return target.getComplement();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getLocalite() {
		return target.getLocalite();
	}

	public String getNumeroMaison() {
		return target.getNumeroMaison();
	}

	public String getRue() {
		return target.getRue();
	}

	public EnumTypeAdresseEntreprise getType() {
		return target.getType();
	}

}

package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.ArrayList;
import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumFormeJuridique;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;

public class PersonneMoraleWrapper implements PersonneMorale {

	private final ch.vd.registre.pm.model.PersonneMorale target;
	private Collection<AdresseEntreprise> adresses = null;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public static PersonneMoraleWrapper get(ch.vd.registre.pm.model.PersonneMorale target) {
		if (target == null) {
			return null;
		}
		return new PersonneMoraleWrapper(target);
	}

	private PersonneMoraleWrapper(ch.vd.registre.pm.model.PersonneMorale target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateConstitution());
		this.dateFin = RegDate.get(target.getDateFinActivite());
	}

	public Collection<AdresseEntreprise> getAdresses() {
		if (adresses == null) {
			adresses = new ArrayList<AdresseEntreprise>();
			final Collection<?> targetAdresses = target.getAdresses();
			if (targetAdresses != null) {
				for (Object o : targetAdresses) {
					ch.vd.registre.pm.model.AdresseEntreprise a = (ch.vd.registre.pm.model.AdresseEntreprise) o;
					adresses.add(AdresseEntrepriseWrapper.get(a));
				}
			}
		}
		return adresses;
	}

	public RegDate getDateConstitution() {
		return dateDebut;
	}

	public RegDate getDateFinActivite() {
		return dateFin;
	}

	public EnumFormeJuridique getFormeJuridique() {
		return target.getFormeJuridique();
	}

	public String getNomContact() {
		return target.getNomContact();
	}

	public String getNumeroCompteBancaire() {
		return target.getNumeroCompteBancaire();
	}

	public long getNumeroEntreprise() {
		return target.getNumeroEntreprise();
	}

	public String getRaisonSociale() {
		return target.getRaisonSociale();
	}

	public String getTelecopieContact() {
		return target.getTelecopieContact();
	}

	public String getTelephoneContact() {
		return target.getTelephoneContact();
	}

	public String getTitulaireCompte() {
		return target.getTitulaireCompte();
	}
}

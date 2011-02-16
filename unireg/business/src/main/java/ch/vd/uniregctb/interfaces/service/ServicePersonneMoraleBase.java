package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;

public abstract class ServicePersonneMoraleBase implements ServicePersonneMoraleService {

	public AdressesPM getAdresses(long noEntreprise, RegDate date) {

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise, PartPM.ADRESSES);
		if (entreprise == null) {
			return null;
		}

		final AdressesPM adresses = new AdressesPM();

		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();
		if (adressesPM != null) {
			for (AdresseEntreprise a : adressesPM) {
				if (isActive(a, date)) {
					adresses.set(a);
				}
			}
		}

		return adresses;
	}

	public AdressesPMHisto getAdressesHisto(long noEntreprise) {

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise, PartPM.ADRESSES);
		if (entreprise == null) {
			return null;
		}

		final AdressesPMHisto adresses = new AdressesPMHisto();

		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();
		if (adressesPM != null) {
			for (AdresseEntreprise a : adressesPM) {
				adresses.add(a);
			}
			adresses.sort();
		}

		return adresses;
	}

	public static boolean isActive(AdresseEntreprise adresse, RegDate date) {
		return RegDateHelper.isBetween(date, adresse.getDateDebutValidite(), adresse.getDateFinValidite(), NullDateBehavior.LATEST);
	}
}

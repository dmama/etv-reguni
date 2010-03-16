package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.pm.service.ServicePersonneMorale;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.wrapper.PersonneMoraleWrapper;

public class ServicePersonneMoraleServiceImpl implements ServicePersonneMoraleService {

	private ServicePersonneMorale servicePersonneMorale;

	public ServicePersonneMoraleServiceImpl() {
		// l'EJB de HostInterface a besoin d'une version 1.5
		JvmVersionHelper.checkJava_1_5();
	}

	public PersonneMorale getPersonneMorale(Long id) {
		try {
			return PersonneMoraleWrapper.get(servicePersonneMorale.getPersonneMorale(id));
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	public void setServicePersonneMorale(ServicePersonneMorale servicePersonneMorale) {
		this.servicePersonneMorale = servicePersonneMorale;
	}

	public AdressesPM getAdresses(long noEntreprise, RegDate date) {

		AdressesPM adresses = new AdressesPM();

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise);
		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();

		for (AdresseEntreprise a : adressesPM) {
			if (isActive(a, date)) {
				adresses.set(a);
			}
		}

		return adresses;
	}

	public AdressesPMHisto getAdressesHisto(long noEntreprise) {

		AdressesPMHisto adresses = new AdressesPMHisto();

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise);
		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();

		for (AdresseEntreprise a : adressesPM) {
			adresses.add(a);
		}

		adresses.sort();
		return adresses;
	}

	public static boolean isActive(AdresseEntreprise adresse, RegDate date) {
		return RegDateHelper.isBetween(date, adresse.getDateDebutValidite(), adresse.getDateFinValidite(), NullDateBehavior.LATEST);
	}
}

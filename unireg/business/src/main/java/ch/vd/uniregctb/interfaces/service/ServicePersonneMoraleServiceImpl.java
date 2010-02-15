package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.pm.service.AttributePM;
import ch.vd.registre.pm.service.ServicePersonneMorale;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.wrapper.PersonneMoraleWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces.EvenementPMWrapper;

public class ServicePersonneMoraleServiceImpl implements ServicePersonneMoraleService {

	private ServicePersonneMorale servicePersonneMorale;

	public ServicePersonneMoraleServiceImpl() {
		// l'EJB de HostInterface a besoin d'une version 1.5
		JvmVersionHelper.checkJava_1_5();
	}

	public void setServicePersonneMorale(ServicePersonneMorale servicePersonneMorale) {
		this.servicePersonneMorale = servicePersonneMorale;
	}

	@SuppressWarnings({"unchecked"})
	public List<Long> getAllIds() {
		try {
			return servicePersonneMorale.getAllNumeros();
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}


	public PersonneMorale getPersonneMorale(Long id) {
		try {
			return PersonneMoraleWrapper.get(servicePersonneMorale.getPersonneMorale(id));
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {
		try {
			return PersonneMoraleWrapper.get(servicePersonneMorale.getPersonneMorale(id, part2attribute(parts)));
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		try {
			final List list = servicePersonneMorale.getPersonnesMorales(ids, part2attribute(parts));
			if (list == null) {
				return Collections.emptyList();
			}

			final List<PersonneMorale> personnes = new ArrayList<PersonneMorale>(list.size());
			for (Object o : list) {
				personnes.add(PersonneMoraleWrapper.get((ch.vd.registre.pm.model.PersonneMorale) o));
			}

			return personnes;
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
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

	public List<EvenementPM> findEvenements(Long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		try {
			List list = servicePersonneMorale.findEvenements(numeroEntreprise, code, RegDate.asJavaDate(minDate), RegDate.asJavaDate(maxDate));
			if (list == null) {
				return Collections.emptyList();
			}

			final List<EvenementPM> events = new ArrayList<EvenementPM>(list.size());
			for (Object e : list) {
				events.add(EvenementPMWrapper.get((ch.vd.registre.pm.model.EvenementPM) e));
			}

			return events;
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	public static boolean isActive(AdresseEntreprise adresse, RegDate date) {
		return RegDateHelper.isBetween(date, adresse.getDateDebutValidite(), adresse.getDateFinValidite(), NullDateBehavior.LATEST);
	}

	private static AttributePM[] part2attribute(PartPM[] parts) {
		if (parts == null) {
			return null;
		}
		final AttributePM[] attributes = new AttributePM[parts.length];
		for (int i = 0, partsLength = parts.length; i < partsLength; i++) {
			final PartPM p = parts[i];
			attributes[i] = AttributePM.valueOf(p.name());
		}
		return attributes;
	}
}

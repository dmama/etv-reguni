package ch.vd.uniregctb.interfaces.service.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.service.AttributePM;
import ch.vd.registre.pm.service.ServicePersonneMorale;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.impl.EtablissementImpl;
import ch.vd.uniregctb.interfaces.model.impl.EvenementPMImpl;
import ch.vd.uniregctb.interfaces.model.impl.PersonneMoraleImpl;
import ch.vd.uniregctb.interfaces.service.PersonneMoraleException;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleBase;

public class ServicePersonneMoraleHostInterfaces extends ServicePersonneMoraleBase {

	private ServicePersonneMorale servicePersonneMorale;

	public ServicePersonneMoraleHostInterfaces() {
		JvmVersionHelper.checkJvmWrtHostInterfaces();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePersonneMorale(ServicePersonneMorale servicePersonneMorale) {
		this.servicePersonneMorale = servicePersonneMorale;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> getAllIds() {
		try {
			return servicePersonneMorale.getAllNumeros();
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	@Override
	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {
		try {
			return PersonneMoraleImpl.get(servicePersonneMorale.getPersonneMorale(id, part2attribute(parts)));
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	@Override
	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		try {
			final List list = servicePersonneMorale.getPersonnesMorales(ids, part2attribute(parts));
			if (list == null) {
				return Collections.emptyList();
			}

			final List<PersonneMorale> personnes = new ArrayList<>(list.size());
			for (Object o : list) {
				personnes.add(PersonneMoraleImpl.get((ch.vd.registre.pm.model.PersonneMorale) o));
			}

			return personnes;
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	@Override
	public Etablissement getEtablissement(long id) {
		try {
			return EtablissementImpl.get(servicePersonneMorale.getEtablissement(id));
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	@Override
	public List<Etablissement> getEtablissements(List<Long> ids) {
		try {
			final List list = servicePersonneMorale.getEtablissement(ids);
			if (list == null) {
				return Collections.emptyList();
			}

			final List<Etablissement> personnes = new ArrayList<>(list.size());
			for (Object o : list) {
				personnes.add(EtablissementImpl.get((ch.vd.registre.pm.model.Etablissement) o));
			}

			return personnes;
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
	}

	@Override
	public List<EvenementPM> findEvenements(long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		try {
			List list = servicePersonneMorale.findEvenements(numeroEntreprise, code, RegDate.asJavaDate(minDate), RegDate.asJavaDate(maxDate));
			if (list == null) {
				return Collections.emptyList();
			}

			final List<EvenementPM> events = new ArrayList<>(list.size());
			for (Object e : list) {
				events.add(EvenementPMImpl.get((ch.vd.registre.pm.model.EvenementPM) e));
			}

			return events;
		}
		catch (Exception e) {
			throw new PersonneMoraleException(e);
		}
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

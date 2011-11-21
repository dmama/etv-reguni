package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.ech.ech0084.v1.PersonInformation;

import ch.vd.evd0001.v3.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class NationaliteRCPers implements Nationalite, Serializable {

	private static final long serialVersionUID = -3520943978958118894L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Pays pays;

	public NationaliteRCPers(Person person, ServiceInfrastructureService infraService) {
		PersonInformation.Nationality nat = person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getNationality();
		this.dateDebut = null; // TODO (rcpers)
		this.dateFin = null; // TODO (rcpers)
		this.pays = initPays(nat, infraService);
	}

	private static Pays initPays(PersonInformation.Nationality nationality, ServiceInfrastructureService infraService) {
		final Pays p;
		final String status = nationality.getNationalityStatus();
		if ("0".equals(status)) {
			// inconnu
			p = infraService.getPaysInconnu();
		}
		else if ("1".equals(status)) {
			// apatride
			p = infraService.getPays(ServiceInfrastructureService.noPaysApatride);
		}
		else if ("2".equals(status)) {
			// ok
			Integer noOfsPays = nationality.getCountryId();
			p = infraService.getPays(noOfsPays);
		}
		else {
			throw new IllegalArgumentException("Code nationality status inconnu = [" + status + "]");
		}
		return p;
	}

	public static Nationalite get(Person person, ServiceInfrastructureService infraService) {
		if (person == null) {
			return null;
		}
		return new NationaliteRCPers(person, infraService);
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public Pays getPays() {
		return pays;
	}
}

package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.ech.ech0011.v5.Nationality;

import ch.vd.evd0001.v3.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;

public class NationaliteRCPers implements Nationalite, Serializable {

	private static final long serialVersionUID = 6509480818877738587L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Pays pays;

	public NationaliteRCPers(Nationality nat, ServiceInfrastructureRaw infraService) {
		this.dateDebut = null;
		this.dateFin = null;
		this.pays = initPays(nat, infraService);
	}

	private static Pays initPays(Nationality nationality, ServiceInfrastructureRaw infraService) {
		final Pays p;
		final String status = nationality.getNationalityStatus();
		if ("0".equals(status)) {
			// inconnu
			p = infraService.getPays(ServiceInfrastructureRaw.noPaysInconnu);
		}
		else if ("1".equals(status)) {
			// apatride
			p = infraService.getPays(ServiceInfrastructureRaw.noPaysApatride);
		}
		else if ("2".equals(status)) {
			// ok
			Integer noOfsPays = nationality.getCountry().getCountryId();
			p = infraService.getPays(noOfsPays);
		}
		else {
			throw new IllegalArgumentException("Code nationality status inconnu = [" + status + ']');
		}
		return p;
	}

	public static Nationalite get(Person person, ServiceInfrastructureRaw infraService) {
		if (person == null || person.getNationality() == null) {
			return null;
		}
		return new NationaliteRCPers(person.getNationality(), infraService);
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

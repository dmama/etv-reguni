package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.vd.evd0001.v4.Nationality;
import ch.vd.evd0001.v4.Person;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;

public class NationaliteRCPers implements Nationalite, Serializable {

	private static final long serialVersionUID = 6762464522148787485L;

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
			p = infraService.getPays(ServiceInfrastructureRaw.noPaysInconnu, null);
		}
		else if ("1".equals(status)) {
			// apatride
			p = infraService.getPays(ServiceInfrastructureRaw.noPaysApatride, null);
		}
		else if ("2".equals(status)) {
			// ok
			Integer noOfsPays = nationality.getCountry().getCountryId();
			p = infraService.getPays(noOfsPays, null);      // TODO jde vérifier que nous utilisons ici la bonne date
		}
		else {
			throw new IllegalArgumentException("Code nationality status inconnu = [" + status + ']');
		}
		return p;
	}

	public static Nationalite get(Person person, ServiceInfrastructureRaw infraService) {
		if (person == null || person.getCurrentNationality() == null) {
			return null;
		}
		return new NationaliteRCPers(person.getCurrentNationality(), infraService);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public Pays getPays() {
		return pays;
	}
}

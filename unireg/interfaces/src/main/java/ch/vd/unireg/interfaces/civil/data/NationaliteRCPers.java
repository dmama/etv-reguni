package ch.vd.unireg.interfaces.civil.data;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

import ch.vd.evd0001.v4.Nationality;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.common.XmlUtils;

public class NationaliteRCPers implements Nationalite, Serializable {

	private static final long serialVersionUID = 1269350960748765702L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Pays pays;

	public NationaliteRCPers(Nationality nat, ServiceInfrastructureRaw infraService) {
		this.pays = initPays(nat, infraService);
		this.dateDebut = determineDateDebut(this.pays, nat.getNaturalizationSwissDate(), nat.getReportingDate());
		this.dateFin = determineDateFin(this.pays, nat.getUndoSwissDate());
	}

	private static Pays initPays(Nationality nationality, ServiceInfrastructureRaw infraService) {
		final Pays p;
		final String status = nationality.getNationalityStatus();
		switch (status) {
		case "0":
			// inconnu
			p = infraService.getPays(ServiceInfrastructureRaw.noPaysInconnu, null);
			break;
		case "1":
			// apatride
			p = infraService.getPays(ServiceInfrastructureRaw.noPaysApatride, null);
			break;
		case "2":
			// ok
			final Integer noOfsPays = nationality.getCountry().getCountryId();
			final RegDate refDate;
			if (noOfsPays == ServiceInfrastructureRaw.noOfsSuisse && nationality.getNaturalizationSwissDate() != null) {
				refDate = XmlUtils.xmlcal2regdate(nationality.getNaturalizationSwissDate());
			}
			else if (noOfsPays == ServiceInfrastructureRaw.noOfsSuisse && nationality.getUndoSwissDate() != null) {
				refDate = XmlUtils.xmlcal2regdate(nationality.getUndoSwissDate());
			}
			else {
				refDate = XmlUtils.xmlcal2regdate(nationality.getReportingDate());
			}
			p = infraService.getPays(noOfsPays, refDate);
			break;
		default:
			throw new IllegalArgumentException("Code nationality status inconnu = [" + status + ']');
		}
		return p;
	}

	private static RegDate determineDateDebut(Pays pays, XMLGregorianCalendar dateDebutSuisse, XMLGregorianCalendar dateReporting) {
		if (pays == null) {
			return null;
		}

		final XMLGregorianCalendar date;
		if (pays.isSuisse() && dateDebutSuisse != null) {
			date = dateDebutSuisse;
		}
		else {
			date = dateReporting;
		}
		return XmlUtils.xmlcal2regdate(date);
	}

	private static RegDate determineDateFin(Pays pays, XMLGregorianCalendar dateFinSuisse) {
		if (pays == null) {
			return null;
		}

		final XMLGregorianCalendar date;
		if (pays.isSuisse() && dateFinSuisse != null) {
			date = dateFinSuisse;
		}
		else {
			date = null;
		}
		return XmlUtils.xmlcal2regdate(date);
	}

	public static Nationalite get(Nationality nationality, ServiceInfrastructureRaw infraService) {
		if (nationality == null) {
			return null;
		}
		return new NationaliteRCPers(nationality, infraService);
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

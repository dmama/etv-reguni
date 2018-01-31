package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.vd.evd0001.v5.ResidencePermit;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.TypePermis;

public class PermisRCPers implements Permis, Serializable {

	private static final long serialVersionUID = -6764765314439659959L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateAnnulation;
	private final RegDate dateValeur;
	private final TypePermis typePermis;

	public PermisRCPers(ResidencePermit permit) {
		final RegDate validFrom = XmlUtils.xmlcal2regdate(permit.getResidencePermitValidFrom());
		final RegDate validTill = XmlUtils.xmlcal2regdate(permit.getResidencePermitTill());
		final RegDate reportingDate = XmlUtils.xmlcal2regdate(permit.getReportingDate());

		// si validFrom est présent, il fait référence
		// sinon, si validFrom est absent,
		//      - si validTill est absent, on prend reportingDate comme validFrom
		//      - si validTill est présent et après reportingDate, on prend reportingDate comme validFrom
		//      - si validTill est présent et avant reportingDate, validFrom reste nulle
		if (validFrom != null) {
			this.dateDebut = validFrom;
		}
		else if (validTill == null || RegDateHelper.isAfter(validTill, reportingDate, NullDateBehavior.LATEST)) {
			this.dateDebut = reportingDate;
		}
		else {
			this.dateDebut = null;
		}
		this.dateFin = validTill;
		this.dateValeur = reportingDate;
		DateRangeHelper.assertValidRange(dateDebut, dateFin, ServiceCivilException.class);
		this.dateAnnulation = null; // les permis annulés ne sont pas exposés par RCPers
		this.typePermis = TypePermis.getFromEvd(permit.getResidencePermit());
	}

	public static Permis get(ResidencePermit permit) {
		if (permit == null) {
			return null;
		}
		return new PermisRCPers(permit);
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
	public RegDate getDateValeur() {
		return dateValeur;
	}

	@Override
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	@Override
	public TypePermis getTypePermis() {
		return typePermis;
	}
}

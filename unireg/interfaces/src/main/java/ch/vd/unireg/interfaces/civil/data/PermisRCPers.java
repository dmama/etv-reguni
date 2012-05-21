package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.vd.evd0001.v3.ResidencePermit;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.TypePermis;

public class PermisRCPers implements Permis, Serializable {

	private static final long serialVersionUID = -5355717389317471235L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateAnnulation;
	private final TypePermis typePermis;

	public PermisRCPers(ResidencePermit permit) {
		this.dateDebut = XmlUtils.xmlcal2regdate(permit.getResidencePermitValidFrom());
		this.dateFin = XmlUtils.xmlcal2regdate(permit.getResidencePermitTill());
		DateRangeHelper.assertValidRange(dateDebut, dateFin, ServiceCivilException.class);
		this.dateAnnulation = null; // les permis annulés ne sont pas exposés par RCPers
		this.typePermis = TypePermis.get(permit.getResidencePermit());
	}

	public static Permis get(ResidencePermit permit) {
		if (permit == null) {
			return null;
		}
		return new PermisRCPers(permit);
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	@Override
	public TypePermis getTypePermis() {
		return typePermis;
	}
}

package ch.vd.uniregctb.webservices.tiers2.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.Range;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class RangeImpl implements Range {

	private Date dateDebut;
	private Date dateFin;

	public RangeImpl(Date dateDebut, Date dateFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public Date getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(Date dateDebut) {
		this.dateDebut = dateDebut;
	}

	public Date getDateFin() {
		return dateFin;
	}

	public void setDateFin(Date dateFin) {
		this.dateFin = dateFin;
	}
}

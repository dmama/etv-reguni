package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Cette classe contient les date de début et de fin d'une période d'imposition <b>au rôle ordinaire</b> d'un contribuable.
 * <p>
 * <b>Note:</b> une période d'imposition est toujours incluse dans une année fiscale (du 1er janvier au 31 décembre). Elle peut être plus
 * courte ou égale, mais pas plus grande.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PeriodeImposition", propOrder = {
		"dateDebut", "dateFin", "idDI"
})
public class PeriodeImposition implements Range {
	/**
	 * La date de début effective de la période d'imposition.
	 */
	@XmlElement(required = true)
	public Date dateDebut;

	/**
	 * La date de fin effective de la période d'imposition.
	 */
	@XmlElement(required = true)
	public Date dateFin;

	/**
	 * L'id de la déclaration d'impôt associée à la période; ou <b>null</b> si la déclaration n'a pas été émise ou a été annulée.
	 */
	@XmlElement(required = false)
	public Long idDI = null;

	public PeriodeImposition() {
	}

	public PeriodeImposition(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periode, Long idDI) {
		this.dateDebut = DataHelper.coreToWeb(periode.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(periode.getDateFin());
		this.idDI = idDI;
	}

	@Override
	public Date getDateDebut() {
		return dateDebut;
	}

	@Override
	public Date getDateFin() {
		return dateFin;
	}

	@Override
	public void setDateDebut(Date v) {
		dateDebut = v;
	}

	@Override
	public void setDateFin(Date v) {
		dateFin = v;
	}
}

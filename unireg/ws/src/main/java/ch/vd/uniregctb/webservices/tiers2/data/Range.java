package ch.vd.uniregctb.webservices.tiers2.data;

/**
 * Interface définissant une plage de dates (début..fin).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface Range {

	Date getDateDebut();

	void setDateDebut(Date v);

	Date getDateFin();

	void setDateFin(Date v);
}

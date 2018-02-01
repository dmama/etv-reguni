package ch.vd.unireg.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public interface AddForView {

	/**
	 * @return l'id du for lui-même; ou <b>null</b> s'il s'agit d'un nouveau for fiscal pas encore sauvé en base de données.
	 */
	Long getId();

	long getTiersId();

	RegDate getDateDebut();

	RegDate getDateFin();

	TypeAutoriteFiscale getTypeAutoriteFiscale();

	Integer getNoAutoriteFiscale();

	String getNomAutoriteFiscale();

	boolean isDateFinFutureAutorisee();

	GenreImpot getGenreImpot();
}

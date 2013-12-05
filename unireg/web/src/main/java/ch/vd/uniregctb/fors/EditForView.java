package ch.vd.uniregctb.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public interface EditForView {

	long getId();

	long getTiersId();

	RegDate getDateDebut();

	RegDate getDateFin();

	TypeAutoriteFiscale getTypeAutoriteFiscale();

	Integer getNoAutoriteFiscale();
}

package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface ForPM extends DateRange {

	@Override
	RegDate getDateDebut();
    
	@Override
	RegDate getDateFin();
    
	int getNoOfsAutoriteFiscale();
    
	TypeNoOfs getTypeAutoriteFiscale();
}

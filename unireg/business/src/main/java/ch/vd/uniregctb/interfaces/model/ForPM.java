package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface ForPM {

	RegDate getDateDebut();
    
	RegDate getDateFin();
    
	int getNoOfsAutoriteFiscale();
    
	TypeNoOfs getTypeAutoriteFiscale();
}

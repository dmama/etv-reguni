package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public interface ValidableRegimeFiscalView extends DateRange {

	RegimeFiscal.Portee getPortee();

	String getCode();
}

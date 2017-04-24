package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public interface ValidableRegimeFiscalView {

	RegDate getDateDebut();

	RegimeFiscal.Portee getPortee();

	String getCode();
}

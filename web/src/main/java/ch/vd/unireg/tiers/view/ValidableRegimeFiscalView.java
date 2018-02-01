package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.RegimeFiscal;

public interface ValidableRegimeFiscalView {

	RegDate getDateDebut();

	RegimeFiscal.Portee getPortee();

	String getCode();
}

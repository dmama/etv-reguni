package ch.vd.uniregctb.migration.pm.engine.data;

import java.util.Collections;
import java.util.NavigableMap;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalCH;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalVD;

/**
 * Container des données à prendre en compte pour les régimes fiscaux d'une entreprise
 */
public class RegimesFiscauxHistoData {

	private final NavigableMap<RegDate, RegpmRegimeFiscalCH> ch;
	private final NavigableMap<RegDate, RegpmRegimeFiscalVD> vd;

	public RegimesFiscauxHistoData(NavigableMap<RegDate, RegpmRegimeFiscalCH> ch, NavigableMap<RegDate, RegpmRegimeFiscalVD> vd) {
		this.ch = ch == null ? Collections.emptyNavigableMap() : ch;
		this.vd = vd == null ? Collections.emptyNavigableMap() : vd;
	}

	public NavigableMap<RegDate, RegpmRegimeFiscalCH> getCH() {
		return ch;
	}

	public NavigableMap<RegDate, RegpmRegimeFiscalVD> getVD() {
		return vd;
	}
}

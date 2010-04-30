package ch.vd.uniregctb.di;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;

/**
 * Helper pour les periodes fiscales
 *
 * @author xcifde
 *
 */
public class DeclarationImpotMapHelperImpl implements DeclarationImpotMapHelper {

	private PeriodeFiscaleDAO periodeFiscaleDAO;

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	@Transactional(readOnly = true)
	public Map<String, String> initMapPeriodeFiscale() {
		Map<String, String> mapPeriodeFiscale = new TreeMap<String, String>();
		List<PeriodeFiscale> periodes = periodeFiscaleDAO.getAllDesc();
		for (PeriodeFiscale periode : periodes) {
			mapPeriodeFiscale.put(periode.getAnnee().toString(), periode.getAnnee().toString());
		}

		return mapPeriodeFiscale;
	}
}

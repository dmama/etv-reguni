package ch.vd.uniregctb.di;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;

public interface DeclarationImpotMapHelper {
	void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO);

	/**
	 * Initialise la map des periodes fiscales
	 * @return une map
	 */
	@Transactional(readOnly = true)
	Map<String, String> initMapPeriodeFiscale();
}

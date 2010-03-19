package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.utils.WebContextUtils;

public class CivilDataManagerImpl extends TiersManager implements CivilDataManager {

	@Transactional(readOnly = true)
	public IndividuView getIndividuView(Long numero) throws AdressesResolutionException, InfrastructureException {

		IndividuView individuView = null;
		if (numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new RuntimeException(this.getMessageSource().getMessage("error.tiers.inexistant", null,
					WebContextUtils.getDefaultLocale()));
		}

		if (tiers != null) {

			if (tiers instanceof PersonnePhysique) {
				PersonnePhysique pp = (PersonnePhysique) tiers;
				individuView = getIndividuView(pp);

			}


		}
		return individuView;
	}
}

package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.individu.IndividuView;

public interface CivilDataManager {

	@Transactional(readOnly = true)
	public IndividuView getIndividuView(Long numero) throws AdressesResolutionException, InfrastructureException;
}

package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.view.AdresseView;

public interface AdresseDataManager {

	@Transactional(rollbackFor = Throwable.class)
	public AdresseView geAdresseViewFromTiers(Long numero) throws AdressesResolutionException, InfrastructureException;
}

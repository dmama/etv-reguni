package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class AdresseDataManagerImpl extends TiersManager implements AdresseDataManager {



	@Transactional(rollbackFor = Throwable.class)
	public AdresseView geAdresseViewFromTiers(Long numero) throws InfrastructureException, AdresseException {
		final Tiers tiers = tiersService.getTiers(numero);
		final AdresseGenerique adresseGenerique = adresseService.getDerniereAdresseVaudoise(tiers, TypeAdresseFiscale.DOMICILE);
		AdresseView adresseView = null;
		if (adresseGenerique != null) {
			adresseView = createAdresseView(adresseGenerique,TypeAdresseTiers.DOMICILE, tiers);
		}
		return adresseView;
	}

}

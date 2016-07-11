package ch.vd.uniregctb.adresse;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Tiers;

/*
 * Cette classe permet d'adapter une adresse supplémentaire (= spécialité UniregCTB) à l'interface d'adresse générique.
 */
public class AdresseSupplementaireAdapter extends AdresseFiscaleAdapter<AdresseSupplementaire> {

	private final boolean isDefault;
	private final Source source;

	public AdresseSupplementaireAdapter(AdresseSupplementaire adresse, Tiers tiers, boolean isDefault, ServiceInfrastructureService service) {
		super(adresse, service);
		this.isDefault = isDefault;
		this.source = new Source(SourceType.FISCALE, tiers);
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	@Override
	public String getNumeroAppartement() {
		return getAdresse().getNumeroAppartement();
	}
}

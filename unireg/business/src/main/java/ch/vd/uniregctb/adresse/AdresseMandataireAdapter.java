package ch.vd.uniregctb.adresse;

import java.util.Collections;
import java.util.Set;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;

/*
 * Cette classe permet d'adapter une adresse supplémentaire (= spécialité UniregCTB) à l'interface d'adresse générique.
 */
public class AdresseMandataireAdapter extends AdresseFiscaleAdapter<AdresseMandataire> {

	private final Source source;

	public AdresseMandataireAdapter(final AdresseMandataire adresse, ServiceInfrastructureService service) {
		super(adresse, service);
		this.source = new Source(SourceType.MANDATAIRE, new Entreprise() {
			@Override
			public Set<DonneeCivileEntreprise> getDonneesCiviles() {
				return Collections.singleton(new RaisonSocialeFiscaleEntreprise(null, null, adresse.getNomDestinataire()));
			}
		});
	}

	@Override
	public String getNumeroAppartement() {
		return null;
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return false;
	}
}

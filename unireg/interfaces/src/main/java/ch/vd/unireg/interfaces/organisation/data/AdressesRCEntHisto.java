package ch.vd.unireg.interfaces.organisation.data;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Container des adresses légales et effectives d'une organisation
 */
public class AdressesRCEntHisto {

	private final List<AdresseLegaleRCEnt> adressesLegales;
	private final List<AdresseEffectiveRCEnt> adressesEffectives;

	public AdressesRCEntHisto(List<AdresseLegaleRCEnt> adressesLegales, List<AdresseEffectiveRCEnt> adressesEffectives) {
		this.adressesLegales = adressesLegales == null ? Collections.<AdresseLegaleRCEnt>emptyList() : adressesLegales;
		this.adressesEffectives = adressesEffectives == null ? Collections.<AdresseEffectiveRCEnt>emptyList() : adressesEffectives;
	}

	@NotNull
	public List<AdresseLegaleRCEnt> getAdressesLegales() {
		return adressesLegales;
	}

	@NotNull
	public List<AdresseEffectiveRCEnt> getAdressesEffectives() {
		return adressesEffectives;
	}
}

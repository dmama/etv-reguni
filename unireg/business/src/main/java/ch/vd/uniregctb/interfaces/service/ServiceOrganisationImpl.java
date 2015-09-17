package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.common.DonneesOrganisationException;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class ServiceOrganisationImpl implements ServiceOrganisationService {

	private ServiceOrganisationRaw target;

	public ServiceOrganisationImpl() {
	}

	public ServiceOrganisationImpl(ServiceOrganisationRaw target) {
		this.target = target;
	}

	public void setTarget(ServiceOrganisationRaw target) {
		this.target = target;
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws DonneesOrganisationException {
		return target.getOrganisationHistory(noOrganisation);
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		return target.getOrganisationPourSite(noSite);
	}

	@Override
	public AdressesCivilesHistoriques getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException {
		final Organisation organisation = getOrganisationHistory(noOrganisation);
		if (organisation == null) {
			return null;
		}

		final AdressesCivilesHistoriques resultat = new AdressesCivilesHistoriques();
		final List<Adresse> adresses = organisation.getAdresses();
		if (adresses != null && !adresses.isEmpty()) {
			for (Adresse adresse : adresses) {
				if (adresse.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
					resultat.courriers.add(adresse);
				}
				else {
					resultat.principales.add(adresse);
				}
			}
		}
		return resultat;
	}
}

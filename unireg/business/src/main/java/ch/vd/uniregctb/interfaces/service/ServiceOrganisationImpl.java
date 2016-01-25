package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.DonneesOrganisationException;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class ServiceOrganisationImpl implements ServiceOrganisationService {

	private ServiceOrganisationRaw target;

	private ServiceInfrastructureService serviceInfra;

	public ServiceOrganisationImpl() {}

	public ServiceOrganisationImpl(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public ServiceOrganisationImpl(ServiceOrganisationRaw target, ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
		this.target = target;
	}

	public void setTarget(ServiceOrganisationRaw target) {
		this.target = target;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
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
		final List<Adresse> adresses = organisation.getAdresses();

		return getAdressesCivilesHistoriques(adresses);
	}

	@NotNull
	protected AdressesCivilesHistoriques getAdressesCivilesHistoriques(List<Adresse> adresses) {
		final AdressesCivilesHistoriques resultat = new AdressesCivilesHistoriques();
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

	@Override
	public AdressesCivilesHistoriques getAdressesSiteOrganisationHisto(long noSite) throws ServiceOrganisationException {
		final SiteOrganisation site = getOrganisationHistory(getOrganisationPourSite(noSite)).getSiteForNo(noSite);
		if (site == null) {
			return null;
		}
		final List<Adresse> adresses =  site.getAdresses();

		return getAdressesCivilesHistoriques(adresses);
	}

	@Override
	@NotNull
	public String createOrganisationDescription(Organisation organisation, RegDate date) {
		Domicile siege = organisation.getSiegePrincipal(date);
		String nomCommune = "";
		if (siege != null) {
			final Commune commune = serviceInfra.getCommuneByNumeroOfs(siege.getNoOfs(), date);
			if (commune != null) {
				nomCommune = commune.getNomOfficielAvecCanton();
			} else {
				nomCommune = serviceInfra.getCommuneByNumeroOfs(siege.getNoOfs(), RegDate.get()).getNomOfficielAvecCanton() + " [actuelle] ";
			}
		}
		FormeLegale formeLegale = organisation.getFormeLegale(date);
		String nom = organisation.getNom(date);
		return String.format("[En date du %s] %s (civil: %d), %s %s, forme juridique %s",
		                     RegDateHelper.dateToDisplayString(date),
		                     nom != null ? nom : "[inconnu]",
		                     organisation.getNumeroOrganisation(),
		                     nomCommune,
		                     siege != null ? "(ofs: " + siege.getNoOfs() + ")" : "[inconnue]",
		                     formeLegale != null ? formeLegale : "[inconnue]");
	}
}

package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.common.DonneesOrganisationException;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class ServiceOrganisationImpl implements ServiceOrganisationService {

	private ServiceOrganisationRaw target;

	private ServiceInfrastructureService serviceInfra;

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


	@Override
	@NotNull
	public String createOrganisationDescription(Organisation organisation, RegDate date) {
		Siege siege = DateRangeHelper.rangeAt(organisation.getSiegesPrincipaux(), date);
		String commune = "";
		if (siege != null) {
			commune = DateRangeHelper.rangeAt(serviceInfra.getCommuneHistoByNumeroOfs(siege.getNoOfs()), date).getNomOfficielAvecCanton();
		}
		DateRanged<FormeLegale> formeLegaleDateRanged = DateRangeHelper.rangeAt(organisation.getFormeLegale(), date);
		DateRanged<String> nomDateRanged = DateRangeHelper.rangeAt(organisation.getNom(), date);
		return String.format("%s (civil: %d), %s %s, forme juridique %s.",
		                     nomDateRanged != null ? nomDateRanged.getPayload() : "[inconnu]",
		                     organisation.getNumeroOrganisation(),
		                     commune,
		                     siege != null ? "(ofs:" + siege.getNoOfs() + ")" : "[inconnue]",
		                     formeLegaleDateRanged != null ? formeLegaleDateRanged.getPayload() : "[inconnue]");
	}
}

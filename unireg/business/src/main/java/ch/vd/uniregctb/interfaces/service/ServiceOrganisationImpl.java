package ch.vd.uniregctb.interfaces.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
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
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		return target.getOrganisationHistory(noOrganisation);
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		return target.getOrganisationEvent(noEvenement);
	}

	@Override
	public ServiceOrganisationRaw.Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		return target.getOrganisationByNoIde(noide);
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
	public AnnonceIDE getAnnonceIDE(Long numero, String userId) throws ServiceOrganisationException {
		final AnnonceIDEQuery annonceIDEQuery = new AnnonceIDEQuery();
		annonceIDEQuery.setNoticeId(numero);
		annonceIDEQuery.setUserId(StringUtils.isBlank(userId) ? RCEntAnnonceIDEHelper.UNIREG_USER : userId);
		final Page<AnnonceIDE> annoncesIDE = target.findAnnoncesIDE(annonceIDEQuery, null, 0, 10);
		final List<AnnonceIDE> content = annoncesIDE.getContent();
		if (content.size() == 0) {
			return null;
		}
		if (content.size() > 1) {
			throw new ServiceOrganisationException("La recherche de l'annonce par son id (" + String.valueOf(numero) + ") a renvoyé plusieurs résultats!");
		}
		return content.get(0);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {
		return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) throws ServiceOrganisationException {
		return target.validerAnnonceIDE(annonceIDE);
	}

	@Override
	@NotNull
	public String createOrganisationDescription(Organisation organisation, RegDate date) {
		final Domicile siege = organisation.getSiegePrincipal(date);
		final String nomCommune;
		if (siege != null) {
			final Commune commune = serviceInfra.getCommuneByNumeroOfs(siege.getNumeroOfsAutoriteFiscale(), date);
			if (commune != null) {
				nomCommune = commune.getNomOfficielAvecCanton();
			}
			else {
				final Commune communeActuelle = serviceInfra.getCommuneByNumeroOfs(siege.getNumeroOfsAutoriteFiscale(), RegDate.get());
				if (communeActuelle != null) {
					nomCommune = communeActuelle.getNomOfficielAvecCanton() + " [actuelle]";
				}
				else {
					nomCommune = "???";
				}
			}
		}
		else {
			nomCommune = StringUtils.EMPTY;
		}
		final FormeLegale formeLegale = organisation.getFormeLegale(date);
		final String nom = organisation.getNom(date);
		final String ide = organisation.getNumeroIDE(date);
		return String.format("[En date du %s] %s (civil: %d), %s %s, IDE %s, forme juridique %s",
		                     RegDateHelper.dateToDisplayString(date),
		                     nom != null ? nom : "[inconnu]",
		                     organisation.getNumeroOrganisation(),
		                     nomCommune,
		                     siege != null ? "(ofs: " + siege.getNumeroOfsAutoriteFiscale() + ")" : "[inconnue]",
		                     StringUtils.defaultIfBlank(FormatNumeroHelper.formatNumIDE(ide), "[inconnu]"),
		                     formeLegale != null ? formeLegale : "[inconnue]");
	}

	/**
	 * Produire un résumé affichable des attributs civil d'un site RCEnt. La raison sociale, le lieu et le numéro IDE si existant sont affichés. Si des données manquent,
	 * un message explicatif est rendu à la place.
	 * @param site Le site RCEnt
	 * @param date La date de validité demandée pour la recherche des attributs
	 * @return Une synthèse des principaux attributs civils.
	 */
	@Override
	public String afficheAttributsSite(@Nullable SiteOrganisation site, @Nullable RegDate date) {
		if (site == null) {
			return "<site introuvable>";
		}
		final RegDate dateToUse = date == null ? RegDate.get() : date;
		final String raisonSociale = site.getNom(dateToUse);
		final String numeroIDE = site.getNumeroIDE(dateToUse);
		final Domicile domicile = site.getDomicile(dateToUse);
		final Integer noOfsDomicile = domicile.getNumeroOfsAutoriteFiscale();
		final Commune commune = serviceInfra.getCommuneByNumeroOfs(noOfsDomicile, dateToUse);
		return String.format("%s, à %s%s",
		                     raisonSociale == null ? "<raison sociale introuvable>" : raisonSociale,
		                     commune == null ? "<commune introuvable pour le numéro OFS " + noOfsDomicile + ">" : commune.getNomOfficielAvecCanton(),
		                     numeroIDE != null ? ", IDE: " + FormatNumeroHelper.formatNumIDE(numeroIDE) : StringUtils.EMPTY);
	}

}

package ch.vd.unireg.interfaces.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;

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
	public Long getNoOrganisationFromNoEtablissement(Long noEtablissement) throws ServiceOrganisationException {
		return target.getNoOrganisationFromNoEtablissement(noEtablissement);
	}

	@Override
	public AdressesCivilesHisto getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException {
		final Organisation organisation = getOrganisationHistory(noOrganisation);
		if (organisation == null) {
			return null;
		}
		final List<Adresse> adresses = organisation.getAdresses();

		return getAdressesCivilesHistoriques(adresses);
	}

	@NotNull
	protected AdressesCivilesHisto getAdressesCivilesHistoriques(List<Adresse> adresses) {
		final AdressesCivilesHisto resultat = new AdressesCivilesHisto();
		if (adresses != null) {
			for (Adresse adresse : adresses) {
				resultat.add(adresse);
			}
		}
		try {
			resultat.finish(false);
		}
		catch (DonneesCivilesException e) {
			throw new ServiceOrganisationException(e);
		}
		return resultat;
	}

	@Nullable
	@Override
	public AdressesCivilesHisto getAdressesEtablissementCivilHisto(long noEtablissement) throws ServiceOrganisationException {
		final EtablissementCivil etablissement = getOrganisationHistory(getNoOrganisationFromNoEtablissement(noEtablissement)).getEtablissementForNo(noEtablissement);
		if (etablissement == null) {
			return null;
		}
		final List<Adresse> adresses =  etablissement.getAdresses();

		return getAdressesCivilesHistoriques(adresses);
	}

	@Nullable
	@Override
	public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) throws ServiceOrganisationException {
		final Page<AnnonceIDE> annoncesIDE = target.findAnnoncesIDE(new AnnonceIDEQuery(numero, userId), null, 0, 10);
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
	 * Produire un résumé affichable des attributs civil d'un établissement civil RCEnt. La raison sociale, le lieu et le numéro IDE si existant sont affichés. Si des données manquent,
	 * un message explicatif est rendu à la place.
	 * @param etablissement L'établissement civil RCEnt
	 * @param date La date de validité demandée pour la recherche des attributs
	 * @return Une synthèse des principaux attributs civils.
	 */
	@Override
	public String afficheAttributsEtablissement(@Nullable EtablissementCivil etablissement, @Nullable RegDate date) {
		if (etablissement == null) {
			return "<établissement civil introuvable>";
		}
		final RegDate dateToUse = date == null ? RegDate.get() : date;
		final String raisonSociale = etablissement.getNom(dateToUse);
		final String numeroIDE = etablissement.getNumeroIDE(dateToUse);
		final Domicile domicile = etablissement.getDomicile(dateToUse);
		final Integer noOfsDomicile = domicile.getNumeroOfsAutoriteFiscale();
		final Commune commune = serviceInfra.getCommuneByNumeroOfs(noOfsDomicile, dateToUse);
		return String.format("%s, à %s%s",
		                     raisonSociale == null ? "<raison sociale introuvable>" : raisonSociale,
		                     commune == null ? "<commune introuvable pour le numéro OFS " + noOfsDomicile + ">" : commune.getNomOfficielAvecCanton(),
		                     numeroIDE != null ? ", IDE: " + FormatNumeroHelper.formatNumIDE(numeroIDE) : StringUtils.EMPTY);
	}

}

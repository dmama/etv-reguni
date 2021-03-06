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
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnectorException;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;

public class ServiceEntrepriseImpl implements ServiceEntreprise {

	private EntrepriseConnector connector;

	private ServiceInfrastructureService serviceInfra;

	public ServiceEntrepriseImpl() {}

	public ServiceEntrepriseImpl(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public ServiceEntrepriseImpl(EntrepriseConnector connector, ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
		this.connector = connector;
	}

	public void setConnector(EntrepriseConnector connector) {
		this.connector = connector;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws EntrepriseConnectorException {
		return connector.getEntrepriseHistory(noEntreprise);
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws EntrepriseConnectorException {
		return connector.getEntrepriseEvent(noEvenement);
	}

	@Override
	public EntrepriseConnector.Identifiers getEntrepriseIdsByNoIde(String noide) throws EntrepriseConnectorException {
		return connector.getEntrepriseByNoIde(noide);
	}

	@Override
	public Long getNoEntrepriseCivileFromNoEtablissementCivil(Long noEtablissement) throws EntrepriseConnectorException {
		return connector.getNoEntrepriseFromNoEtablissement(noEtablissement);
	}

	@Override
	public AdressesCivilesHisto getAdressesEntrepriseHisto(long noEntrepriseCivile) throws EntrepriseConnectorException {
		final EntrepriseCivile entrepriseCivile = getEntrepriseHistory(noEntrepriseCivile);
		if (entrepriseCivile == null) {
			return null;
		}
		final List<Adresse> adresses = entrepriseCivile.getAdresses();

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
			throw new EntrepriseConnectorException(e);
		}
		return resultat;
	}

	@Nullable
	@Override
	public AdressesCivilesHisto getAdressesEtablissementCivilHisto(long noEtablissementCivil) throws EntrepriseConnectorException {
		final EtablissementCivil etablissement = getEntrepriseHistory(getNoEntrepriseCivileFromNoEtablissementCivil(noEtablissementCivil)).getEtablissementForNo(noEtablissementCivil);
		if (etablissement == null) {
			return null;
		}
		final List<Adresse> adresses =  etablissement.getAdresses();

		return getAdressesCivilesHistoriques(adresses);
	}

	@Nullable
	@Override
	public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) throws EntrepriseConnectorException {
		final Page<AnnonceIDE> annoncesIDE = connector.findAnnoncesIDE(new AnnonceIDEQuery(numero, userId), null, 0, 10);
		final List<AnnonceIDE> content = annoncesIDE.getContent();
		if (content.size() == 0) {
			return null;
		}
		if (content.size() > 1) {
			throw new EntrepriseConnectorException("La recherche de l'annonce par son id (" + String.valueOf(numero) + ") a renvoyé plusieurs résultats!");
		}
		return content.get(0);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws EntrepriseConnectorException {
		return connector.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) throws EntrepriseConnectorException {
		return connector.validerAnnonceIDE(annonceIDE);
	}

	@Override
	@NotNull
	public String createEntrepriseDescription(EntrepriseCivile entrepriseCivile, RegDate date) {
		final Domicile siege = entrepriseCivile.getSiegePrincipal(date);
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
		final FormeLegale formeLegale = entrepriseCivile.getFormeLegale(date);
		final String nom = entrepriseCivile.getNom(date);
		final String ide = entrepriseCivile.getNumeroIDE(date);
		return String.format("[En date du %s] %s (civil: %d), %s %s, IDE %s, forme juridique %s",
		                     RegDateHelper.dateToDisplayString(date),
		                     nom != null ? nom : "[inconnu]",
		                     entrepriseCivile.getNumeroEntreprise(),
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

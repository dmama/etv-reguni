package ch.vd.uniregctb.indexer.tiers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.OrganisationNotFoundException;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SiteOrganisationNotFoundException;
import ch.vd.uniregctb.tiers.TiersService;

public class EtablissementIndexable extends ContribuableIndexable<Etablissement> {

	public static final String SUB_TYPE = "etablissement";

	private final SiteOrganisation site;

	public EtablissementIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceInfrastructureService serviceInfra,
	                              ServiceOrganisationService serviceOrganisation, AvatarService avatarService, Etablissement etablissement) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, etablissement);

		if (etablissement.isConnuAuCivil()) {
			final Long noOrganisation = serviceOrganisation.getOrganisationPourSite(etablissement.getNumeroEtablissement());
			if (noOrganisation != null) {
				final Organisation organisation = serviceOrganisation.getOrganisationHistory(noOrganisation);
				if (organisation == null) {
					throw new OrganisationNotFoundException(noOrganisation);
				}

				this.site = findSite(organisation, etablissement.getNumeroEtablissement());
				if (this.site == null) {
					throw new SiteOrganisationNotFoundException(etablissement);
				}
			}
			else {
				throw new SiteOrganisationNotFoundException(etablissement);
			}
		}
		else {
			this.site = null;
		}
	}

	@Nullable
	private static SiteOrganisation findSite(Organisation organisation, long noSite) {
		final List<SiteOrganisation> sites = organisation.getDonneesSites();
		if (sites == null || sites.isEmpty()) {
			return null;
		}
		for (SiteOrganisation candidat : sites) {
			if (candidat.getNumeroSite() == noSite) {
				return candidat;
			}
		}
		// pas trouvé...
		return null;
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillIdeData(TiersIndexableData data) {
		// [SIFISC-18106] seul(s) le(s) numéro(s) IDE porté(s) par l'établissement en question doivent être visible ici
		if (site == null) {
			// récupération des données présentes en base de données Unireg
			super.fillIdeData(data);
		}
		else if (site.getNumeroIDE() != null) {
			for (DateRanged<String> ide : site.getNumeroIDE()) {
				data.addIde(ide.getPayload());
			}
		}
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setConnuAuCivil(tiers.isConnuAuCivil());

		if (site == null) {
			data.addAutresNom(tiers.getEnseigne());
			data.addNomRaison(tiers.getRaisonSociale());
			data.addNom1(tiers.getRaisonSociale());
		}
		else {
			final List<DateRanged<String>> noms = site.getNom();
			for (DateRanged<String> nom : noms) {
				data.addNomRaison(nom.getPayload());
			}
			data.addNom1(noms.get(noms.size() - 1).getPayload());
		}

		// qualification du type principal/secondaire (attention ! même les rapports annulés sont pris en compte !)
		for (RapportEntreTiers ret : tiers.getRapportsObjet()) {
			if (ret instanceof ActiviteEconomique) {
				data.addTypeEtablissement(((ActiviteEconomique) ret).isPrincipal() ? TypeEtablissement.PRINCIPAL : TypeEtablissement.SECONDAIRE);
			}
		}

		// éventuels identifiants RC (en provenance du civil seulement)
		if (site != null) {
			final List<DateRanged<String>> all = site.getNumeroRC();
			if (all != null) {
				all.stream()
						.map(DateRanged::getPayload)
						.map(IndexerFormatHelper::numRCToString)
						.distinct()
						.forEach(data::addNumeroRC);
			}
		}
	}
}

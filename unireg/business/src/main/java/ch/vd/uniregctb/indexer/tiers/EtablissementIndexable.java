package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.OrganisationNotFoundException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SiteOrganisationNotFoundException;
import ch.vd.uniregctb.tiers.TiersService;

public class EtablissementIndexable extends ContribuableIndexable<Etablissement> {

	public static final String SUB_TYPE = "etablissement";

	private final Organisation organisation;
	private final SiteOrganisation site;

	private final List<ContribuableIndexable<?>> entitesJuridiques;

	public EtablissementIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, ServiceOrganisationService serviceOrganisation,
	                              ServiceCivilService serviceCivil, AvatarService avatarService, Etablissement etablissement) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, etablissement);

		if (etablissement.isConnuAuCivil()) {
			final Long noOrganisation = serviceOrganisation.getOrganisationPourSite(etablissement.getNumeroEtablissement());
			if (noOrganisation != null) {
				this.organisation = serviceOrganisation.getOrganisationHistory(noOrganisation);
				if (organisation == null) {
					throw new OrganisationNotFoundException(noOrganisation);
				}

				this.site = findSite(this.organisation, etablissement.getNumeroEtablissement());
				if (site == null) {
					throw new SiteOrganisationNotFoundException(etablissement);
				}
			}
			else {
				throw new SiteOrganisationNotFoundException(etablissement);
			}
		}
		else {
			this.organisation = null;
			this.site = null;
		}

		// les entités juridiques qui se cachent derrière l'établissement
		final List<DateRanged<Contribuable>> entites = tiersService.getEntitesJuridiquesEtablissement(etablissement);
		if (entites != null && !entites.isEmpty()) {
			entitesJuridiques = new ArrayList<>(entites.size());
			for (DateRanged<Contribuable> range : entites) {
				final Contribuable ctb = range.getPayload();
				if (ctb instanceof PersonnePhysique) {
					final PersonnePhysique pp = (PersonnePhysique) ctb;
					if (pp.isHabitantVD()) {
						final Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), null, AttributeIndividu.ADRESSES);
						if (ind == null) {
							throw new IndividuNotFoundException(pp);
						}
						entitesJuridiques.add(new HabitantIndexable(adresseService, tiersService, serviceInfra, avatarService, pp, ind));
					}
					else {
						entitesJuridiques.add(new NonHabitantIndexable(adresseService, tiersService, serviceInfra, avatarService, pp));
					}
				}
				else if (ctb instanceof Entreprise) {
					final Entreprise entreprise = (Entreprise) ctb;
					entitesJuridiques.add(new EntrepriseIndexable(adresseService, tiersService, serviceInfra, serviceOrganisation, avatarService, entreprise));
				}
			}
		}
		else {
			entitesJuridiques = Collections.emptyList();
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
		// on met l'IDE de l'entreprise parente et l'IDE de l'établissement, s'il existe

		// 1. celui de l'établissement
		if (site == null) {
			// récupération des données présentes en base de données Unireg
			super.fillIdeData(data);
		}
		else if (site.getNumeroIDE() != null) {
			for (DateRanged<String> ide : site.getNumeroIDE()) {
				data.addIde(ide.getPayload());
			}
		}

		// 2. celui de l'entreprise parente (ou de toutes si elles sont plusieurs)
		if (organisation == null) {
			for (ContribuableIndexable<?> ctb : entitesJuridiques) {
				final TiersIndexableData ctbData = (TiersIndexableData) ctb.getIndexableData();
				data.addIde(ctbData.getIde());
			}
		}
		else if (organisation.getNumeroIDE() != null) {
			for (DateRanged<String> ide : organisation.getNumeroIDE()) {
				data.addIde(ide.getPayload());
			}
		}
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

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
	}
}

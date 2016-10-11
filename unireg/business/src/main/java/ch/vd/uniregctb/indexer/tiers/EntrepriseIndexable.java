package ch.vd.uniregctb.indexer.tiers;

import java.util.List;
import java.util.Set;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.OrganisationNotFoundException;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class EntrepriseIndexable extends ContribuableImpositionPersonnesMoralesIndexable<Entreprise> {

	public static final String SUB_TYPE = "entreprise";

	private final Organisation organisation;

	public EntrepriseIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceInfrastructureService serviceInfra,
	                           ServiceOrganisationService serviceOrganisationService, AvatarService avatarService, Entreprise entreprise) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, entreprise);
		if (entreprise.isConnueAuCivil()) {
			this.organisation = serviceOrganisationService.getOrganisationHistory(entreprise.getNumeroEntreprise());
			if (this.organisation == null) {
				throw new OrganisationNotFoundException(entreprise);
			}
		}
		else {
			this.organisation = null;
		}
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setNatureJuridique(IndexerFormatHelper.enumToString(NatureJuridique.PM));
		data.setConnuAuCivil(tiers.isConnueAuCivil());

		// les noms
		final List<RaisonSocialeHisto> rss = tiersService.getRaisonsSociales(tiers, false);
		if (!rss.isEmpty()) {
			for (RaisonSocialeHisto histo : rss) {
				data.addNomRaison(histo.getRaisonSociale());
			}
			data.setNom1(rss.get(rss.size() - 1).getRaisonSociale());
		}

		// l'historique du nom additionnel (en provenance du civil seulement)
		if (organisation != null) {
			final List<DateRanged<String>> historiqueNomAdditionnel = organisation.getNomAdditionnel();
			if (historiqueNomAdditionnel != null && !historiqueNomAdditionnel.isEmpty()) {
				for (DateRanged<String> nomAdditionnel : historiqueNomAdditionnel) {
					data.addAutresNom(nomAdditionnel.getPayload());
				}
			}
		}

		// la forme juridique
		final List<FormeLegaleHisto> fjs = tiersService.getFormesLegales(tiers, false);
		if (!fjs.isEmpty()) {
			final FormeLegaleHisto fj = fjs.get(fjs.size() - 1);
			data.setFormeJuridique(fj.getFormeLegale().getCode());
			data.setCategorieEntreprise(IndexerFormatHelper.enumToString(CategorieEntrepriseHelper.map(fj.getFormeLegale())));
		}

		// la date de création / d'inscription RC
		data.addDateNaissance(tiersService.getDateCreation(tiers));

		// le siège (= domicile de l'établissement principal)
		final List<DomicileHisto> sieges = tiersService.getSieges(tiers, false);
		if (sieges != null && !sieges.isEmpty()) {
			final DomicileHisto dernier = CollectionsUtils.getLastElement(sieges);
			data.setDomicileEtablissementPrincipal(getLocalisationAsString(dernier, tiers));
		}

		// les états de l'entreprise (état actuel + états historiques)
		final List<EtatEntreprise> etatsEntreprise = tiers.getEtatsNonAnnulesTries();
		final EtatEntreprise etatActuel = etatsEntreprise.isEmpty() ? null : CollectionsUtils.getLastElement(etatsEntreprise);
		if (etatActuel != null) {
			data.setEtatEntrepriseCourant(etatActuel.getType());
		}
		for (EtatEntreprise etat : etatsEntreprise) {
			if (etat != null) {
				data.addEtatEntreprise(etat.getType());
			}
		}

		// les spécificités du RC pour les états : on veut pouvoir rechercher sur l'état actuel de l'inscription RC
		for (EtatEntreprise etat : CollectionsUtils.revertedOrder(etatsEntreprise)) {
			if (etat.getType() == TypeEtatEntreprise.RADIEE_RC) {
				data.setEtatInscriptionRC(TypeEtatInscriptionRC.RADIEE);
				break;
			}
			if (etat.getType() == TypeEtatEntreprise.INSCRITE_RC) {
				data.setEtatInscriptionRC(TypeEtatInscriptionRC.ACTIVE);
				break;
			}
		}

		// particularité... cette entreprise a-t-elle absorbé d'autres entreprises par le passé ?
		final Set<RapportEntreTiers> rapportsObjets = tiers.getRapportsObjet();
		boolean isMergeResult = false;
		if (rapportsObjets != null) {
			for (RapportEntreTiers rapport : rapportsObjets) {
				if (!rapport.isAnnule() && rapport.getType() == TypeRapportEntreTiers.FUSION_ENTREPRISES) {
					isMergeResult = true;
					break;
				}
			}
		}
		data.setCorporationMergeResult(isMergeResult);

		// même chose pour les booléens "a été scindée" et "a transféré du patrimoine chez quelqu'un d'autre"
		final Set<RapportEntreTiers> rapportsSujets = tiers.getRapportsSujet();
		boolean isSplit = false;
		boolean hasTransferedPatrimony = false;
		if (rapportsSujets != null) {
			for (RapportEntreTiers rapport : rapportsSujets) {
				if (!rapport.isAnnule() && rapport.getType() == TypeRapportEntreTiers.SCISSION_ENTREPRISE) {
					isSplit = true;
				}
				if (!rapport.isAnnule() && rapport.getType() == TypeRapportEntreTiers.TRANSFERT_PATRIMOINE) {
					hasTransferedPatrimony = true;
				}
				if (isSplit && hasTransferedPatrimony) {
					// plus vraiment de perpective de changement, ou bien ?
					break;
				}
			}
		}
		data.setCorporationSplit(isSplit);
		data.setCorporationTransferedPatrimony(hasTransferedPatrimony);
	}

	@Override
	protected void fillIdeData(TiersIndexableData data) {
		if (organisation != null) {
			for (DateRanged<String> ide : organisation.getNumeroIDE()) {
				data.addIde(ide.getPayload());
			}
		}
		else {
			super.fillIdeData(data);
		}
	}
}

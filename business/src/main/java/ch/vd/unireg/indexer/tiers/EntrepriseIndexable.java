package ch.vd.unireg.indexer.tiers;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.IndexerFormatHelper;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EntrepriseNotFoundException;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FormeLegaleHisto;
import ch.vd.unireg.tiers.RaisonSocialeHisto;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.NatureJuridique;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class EntrepriseIndexable extends ContribuableImpositionPersonnesMoralesIndexable<Entreprise> {

	public static final String SUB_TYPE = "organisation";

	private final EntrepriseCivile entrepriseCivile;

	public EntrepriseIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceInfrastructureService serviceInfra,
	                           ServiceEntreprise serviceEntreprise, AvatarService avatarService, Entreprise entreprise) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, entreprise);

		if (entreprise.isConnueAuCivil()) {
			this.entrepriseCivile = serviceEntreprise.getEntrepriseHistory(entreprise.getNumeroEntreprise());
			if (this.entrepriseCivile == null) {
				throw new EntrepriseNotFoundException(entreprise);
			}
		}
		else {
			this.entrepriseCivile = null;
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
		if (entrepriseCivile != null) {
			final List<DateRanged<String>> historiqueNomAdditionnel = entrepriseCivile.getNomAdditionnel();
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
			final CategorieEntreprise currentCategorie = tiersService.getCategorieEntreprise(tiers, RegDate.get());
			data.setCategorieEntreprise(IndexerFormatHelper.enumToString(currentCategorie));
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

		// éventuels identifiants RC (en provenance du civil seulement)
		if (entrepriseCivile != null) {
			final List<DateRanged<String>> all = entrepriseCivile.getNumeroRC();
			if (all != null) {
				all.stream()
						.map(DateRanged::getPayload)
						.map(IndexerFormatHelper::numRCToString)
						.distinct()
						.forEach(data::addNumeroRC);
			}
		}
	}

	@Override
	protected void fillIdeData(TiersIndexableData data) {
		if (entrepriseCivile != null) {
			for (DateRanged<String> ide : entrepriseCivile.getNumeroIDE()) {
				data.addIde(ide.getPayload());
			}
		}
		else {
			super.fillIdeData(data);
		}
	}
}

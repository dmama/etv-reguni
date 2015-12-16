package ch.vd.uniregctb.indexer.tiers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.OrganisationNotFoundException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.NatureJuridique;

public class EntrepriseIndexable extends ContribuableIndexable<Entreprise> {

	public static final String SUB_TYPE = "entreprise";

	private final Organisation organisation;

	public EntrepriseIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, ServiceOrganisationService serviceOrganisationService,
	                           AvatarService avatarService, Entreprise entreprise) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, entreprise);
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
		if (organisation != null) {

			// l'information vient donc du civil

			// les noms de l'entreprise
			final List<DateRanged<String>> historiqueNoms = organisation.getNom();
			if (historiqueNoms != null && !historiqueNoms.isEmpty()) {
				for (DateRanged<String> nom : historiqueNoms) {
					data.addNomRaison(nom.getPayload());
				}
				data.setNom1(historiqueNoms.get(historiqueNoms.size() - 1).getPayload());
			}
			final Map<String, List<DateRanged<String>>> historiqueNomsAdditionels = organisation.getNomsAdditionnels();
			if (historiqueNomsAdditionels != null && !historiqueNomsAdditionels.isEmpty()) {
				for (String nomAdditionnel : historiqueNomsAdditionels.keySet()) {
						data.addAutresNom(nomAdditionnel);
				}
			}
			FormeLegale formeLegale = organisation.getFormeLegale(null);
			if (formeLegale != null) {
				data.setFormeJuridique(formeLegale.getCode());
				data.setCategorieEntreprise(IndexerFormatHelper.enumToString(CategorieEntrepriseHelper.map(formeLegale)));
			}
			/*
			 Cependant, les codes sont censés correspondre car il s'agit dans les deux cas des codes eCH à 2 ou 4 chiffres.
			 */
		}
		else {
			// ok, on prend tout ce qu'on a au fiscal

			// les noms de l'entreprise
			final List<DonneesRegistreCommerce> donneesRC = tiers.getDonneesRegistreCommerceNonAnnuleesTriees();
			final Set<String> noms = new HashSet<>(donneesRC.size());
			for (DonneesRegistreCommerce drc : donneesRC) {
				noms.add(drc.getRaisonSociale());
			}
			for (String nom : noms) {
				data.addNomRaison(nom);
			}
			if (!donneesRC.isEmpty()) {
				data.setNom1(donneesRC.get(donneesRC.size() - 1).getRaisonSociale());
			}
			// FIXME: voir remarque ci-dessus.
			if (!donneesRC.isEmpty()) {
				FormeJuridiqueEntreprise formeJuridique = donneesRC.get(donneesRC.size() - 1).getFormeJuridique();
				if (formeJuridique != null) {
					data.setFormeJuridique(formeJuridique.getCodeECH());
					data.setCategorieEntreprise(IndexerFormatHelper.enumToString(CategorieEntrepriseHelper.map(formeJuridique)));
				}
			}
		}
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

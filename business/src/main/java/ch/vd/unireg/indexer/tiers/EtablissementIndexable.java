package ch.vd.unireg.indexer.tiers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.IndexerFormatHelper;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.EntrepriseNotFoundException;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.EtablissementCivilNotFoundException;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.TiersService;

public class EtablissementIndexable extends ContribuableIndexable<Etablissement> {

	public static final String SUB_TYPE = "etablissement";

	private final EtablissementCivil etablissement;

	public EtablissementIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceInfrastructureService serviceInfra,
	                              ServiceEntreprise serviceEntreprise, AvatarService avatarService, Etablissement etablissement) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, etablissement);

		if (etablissement.isConnuAuCivil()) {
			final Long noEntrepriseCivil = serviceEntreprise.getNoEntrepriseCivileFromNoEtablissementCivil(etablissement.getNumeroEtablissement());
			if (noEntrepriseCivil != null) {
				final EntrepriseCivile entrepriseCivile = serviceEntreprise.getEntrepriseHistory(noEntrepriseCivil);
				if (entrepriseCivile == null) {
					throw new EntrepriseNotFoundException(noEntrepriseCivil);
				}

				this.etablissement = findEtablissementCivil(entrepriseCivile, etablissement.getNumeroEtablissement());
				if (this.etablissement == null) {
					throw new EtablissementCivilNotFoundException(etablissement);
				}
			}
			else {
				throw new EtablissementCivilNotFoundException(etablissement);
			}
		}
		else {
			this.etablissement = null;
		}
	}

	@Nullable
	private static EtablissementCivil findEtablissementCivil(EntrepriseCivile entrepriseCivile, long noEtablissement) {
		final List<EtablissementCivil> etablissements = entrepriseCivile.getEtablissements();
		if (etablissements == null || etablissements.isEmpty()) {
			return null;
		}
		for (EtablissementCivil candidat : etablissements) {
			if (candidat.getNumeroEtablissement() == noEtablissement) {
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
		if (etablissement == null) {
			// récupération des données présentes en base de données Unireg
			super.fillIdeData(data);
		}
		else if (etablissement.getNumeroIDE() != null) {
			for (DateRanged<String> ide : etablissement.getNumeroIDE()) {
				data.addIde(ide.getPayload());
			}
		}
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setConnuAuCivil(tiers.isConnuAuCivil());

		if (etablissement == null) {
			data.addAutresNom(tiers.getEnseigne());
			data.addNomRaison(tiers.getRaisonSociale());
			data.addNom1(tiers.getRaisonSociale());
		}
		else {
			final List<DateRanged<String>> noms = etablissement.getNom();
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
		if (etablissement != null) {
			final List<DateRanged<String>> all = etablissement.getNumeroRC();
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

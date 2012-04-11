package ch.vd.uniregctb.indexer.tiers;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.AssujettissementPM;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.helper.EntrepriseHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EntrepriseIndexable extends ContribuableIndexable {

	public static final String SUB_TYPE = "entreprise";

	private final PersonneMorale pm;

	public EntrepriseIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, ServicePersonneMoraleService servicePM,
	                           Entreprise entreprise) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, entreprise);
		this.pm = servicePM.getPersonneMorale(entreprise.getNumero(), PartPM.ADRESSES, PartPM.FORS_FISCAUX, PartPM.ASSUJETTISSEMENTS);
		if (pm == null) {
			throw new IndexerException("Impossible de trouver la personne morale n°" + entreprise.getNumero() + " dans le registre PM.");
		}
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setNatureJuridique(IndexerFormatHelper.objectToString(NatureJuridique.PM));
		data.addNomRaison(pm.getRaisonSociale1());
		data.addNomRaison(pm.getRaisonSociale2());
		data.addNomRaison(pm.getRaisonSociale3());
		data.setNom1(pm.getRaisonSociale());

		final String role;
		if (isAssujettiAt(pm.getAssujettissementsLIC(), null)) {
			role = "Assujetti";
		}
		else if (isAssujettiAt(pm.getAssujettissementsLIFD(), null)) {
			role = "Assujetti";
		}
		else {
			role = "Non assujetti";
		}
		data.setRoleLigne2(role);
	}

	private boolean isAssujettiAt(List<AssujettissementPM> list, RegDate date) {
		if (list != null && !list.isEmpty()) {
			for (AssujettissementPM a : list) {
				if (a.isValidAt(date)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void fillForsData(TiersIndexableData data) {

		// For principal actif
		boolean isActif = false;
		String typeAutFfpActif = null;
		String noOfsFfpActif = null;

		// Dernier for principal
		String communeDernierFfp = null;
		RegDate dateOuvertureFor = null;
		RegDate dateFermetureFor = null;

		final List<ForPM> forsPrincipaux = pm.getForsFiscauxPrincipaux();
		if (forsPrincipaux != null && !forsPrincipaux.isEmpty()) {
			final ForPM ffp = forsPrincipaux.get(forsPrincipaux.size() - 1);

			isActif = ffp.isValidAt(null); // le dernier for principal (les fors fiscaux annulés ne sont pas exposés par host-interfaces)
			if (isActif) {
				final TypeAutoriteFiscale type = getTypeAutoriteFiscaleForPM(ffp);
				typeAutFfpActif = type.toString();
				noOfsFfpActif = String.valueOf(ffp.getNoOfsAutoriteFiscale());
			}

			dateOuvertureFor = ffp.getDateDebut();
			dateFermetureFor = ffp.getDateFin();

			final Commune commune = getCommuneForPM(ffp);
			if (commune != null) {
				communeDernierFfp = commune.getNomMinuscule();
			}
		}

		// Autre fors
		final StringBuilder noOfsAutresFors = new StringBuilder();
		final List<ForPM> forsSecondaires = pm.getForsFiscauxSecondaires();
		if (forsSecondaires != null) {
			for (ForPM ffs : forsSecondaires) {
				addValue(noOfsAutresFors, String.valueOf(ffs.getNoOfsAutoriteFiscale()));
			}
		}

		data.setTiersActif(IndexerFormatHelper.objectToString(isActif));
		data.setNoOfsForPrincipal(noOfsFfpActif);
		data.setTypeOfsForPrincipal(typeAutFfpActif);
		data.setNosOfsAutresFors(noOfsAutresFors.toString());
		data.setForPrincipal(communeDernierFfp);
		data.setDateOuvertureFor(IndexerFormatHelper.objectToString(dateOuvertureFor));
		data.setDateFermtureFor(IndexerFormatHelper.objectToString(dateFermetureFor));
	}

	private TypeAutoriteFiscale getTypeAutoriteFiscaleForPM(ForPM ffp) {
		return EntrepriseHelper.getTypeAutoriteFiscaleForPM(ffp, serviceInfra);
	}

	private Commune getCommuneForPM(ForPM ffp) {
		return EntrepriseHelper.getCommuneForPM(ffp, serviceInfra);
	}
}

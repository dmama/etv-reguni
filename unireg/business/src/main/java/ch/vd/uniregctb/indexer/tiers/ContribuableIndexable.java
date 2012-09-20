package ch.vd.uniregctb.indexer.tiers;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ContribuableIndexable extends TiersIndexable {

	// private static final Logger LOGGER = Logger.getLogger(ContribuableIndexable.class);

	public ContribuableIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, Contribuable contribuable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, contribuable);
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
		if (ffp != null) {
			final boolean isActif = (ffp.isValidAt(null));
			data.setModeImposition(IndexerFormatHelper.objectToString(ffp.getModeImposition()));
			data.setTiersActif(IndexerFormatHelper.objectToString(isActif));
		}
		else {
			data.setTiersActif(IndexerFormatHelper.objectToString(false));
		}
	}

	@Override
	protected void fillForsData(TiersIndexableData data) {

		// For principal actif
		String typeAutFfpActif = null;
		String noOfsFfpActif = null;

		final ForFiscalPrincipal principalActif = tiers.getForFiscalPrincipalAt(null);
		if (principalActif != null) {
			typeAutFfpActif = principalActif.getTypeAutoriteFiscale().toString();
			noOfsFfpActif = principalActif.getNumeroOfsAutoriteFiscale().toString();
		}

		// For principal
		String communeDernierFfp = null;
		RegDate dateOuvertureFor = null;
		RegDate dateFermetureFor = null;
		final ForFiscalPrincipal dernierPrincipal = tiers.getDernierForFiscalPrincipal();
		if (dernierPrincipal != null) {
			communeDernierFfp = getForCommuneAsString(dernierPrincipal);
			dateOuvertureFor = dernierPrincipal.getDateDebut();
			dateFermetureFor = dernierPrincipal.getDateFin();
		}

		// Autre fors
		StringBuilder noOfsAutresFors = new StringBuilder();
		final Set<ForFiscal> fors = tiers.getForsFiscaux();
		if (fors != null) {
			for (ForFiscal forF : fors) {
				addValue(noOfsAutresFors, forF.getNumeroOfsAutoriteFiscale().toString());
			}
		}

		data.setNoOfsForPrincipal(noOfsFfpActif);
		data.setTypeOfsForPrincipal(typeAutFfpActif);
		data.setNosOfsAutresFors(noOfsAutresFors.toString());
		data.setForPrincipal(communeDernierFfp);
		data.setDateOuvertureFor(IndexerFormatHelper.objectToString(dateOuvertureFor));
		data.setDateFermtureFor(IndexerFormatHelper.objectToString(dateFermetureFor));
	}
}


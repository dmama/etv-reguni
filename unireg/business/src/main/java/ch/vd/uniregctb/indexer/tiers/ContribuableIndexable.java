package ch.vd.uniregctb.indexer.tiers;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ContribuableIndexable<T extends Contribuable> extends TiersIndexable<T> {

	// private static final Logger LOGGER = LoggerFactory.getLogger(ContribuableIndexable.class);

	public ContribuableIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, AvatarService avatarService, T contribuable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, contribuable);
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
		if (ffp != null) {
			final boolean isActif = ffp.isValidAt(null);
			if (ffp instanceof ForFiscalPrincipalPP) {
				data.setModeImposition(IndexerFormatHelper.enumToString(((ForFiscalPrincipalPP) ffp).getModeImposition()));
			}
			data.setTiersActif(IndexerFormatHelper.booleanToString(isActif));
		}
		else {
			data.setTiersActif(IndexerFormatHelper.booleanToString(false));
		}

		fillIdeData(data);
	}

	protected void fillIdeData(TiersIndexableData data) {
		final Set<IdentificationEntreprise> ides = tiers.getIdentificationsEntreprise();
		if (ides != null && !ides.isEmpty()) {
			for (IdentificationEntreprise ide : ides) {
				data.addIde(ide.getNumeroIde());
			}
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

		// Fors vaudois
		RegDate dateOuvertureForVd = null;
		RegDate dateFermetureForVd = null;
		ForFiscal premierVd = tiers.getPremierForFiscalVd();
		if (premierVd != null) {
			dateOuvertureForVd = premierVd.getDateDebut();
		}
		ForFiscal dernierVd = tiers.getDernierForFiscalVd();
		if (dernierVd != null) {
			dateFermetureForVd = dernierVd.getDateFin();
		}


		data.setNoOfsForPrincipal(noOfsFfpActif);
		data.setTypeOfsForPrincipal(typeAutFfpActif);
		data.setNosOfsAutresFors(noOfsAutresFors.toString());
		data.setForPrincipal(communeDernierFfp);
		data.setDateOuvertureFor(IndexerFormatHelper.dateToString(dateOuvertureFor, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setDateFermtureFor(IndexerFormatHelper.dateToString(dateFermetureFor, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setDateOuvertureForVd(IndexerFormatHelper.dateToString(dateOuvertureForVd, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setDateFermtureForVd(IndexerFormatHelper.dateToString(dateFermetureForVd, IndexerFormatHelper.DateStringMode.STORAGE));
	}
}


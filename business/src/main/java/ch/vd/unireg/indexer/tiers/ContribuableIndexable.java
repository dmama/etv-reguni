package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class ContribuableIndexable<T extends Contribuable> extends TiersIndexable<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContribuableIndexable.class);

	private final AssujettissementService assujettissementService;

	public ContribuableIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceInfrastructureService serviceInfra, AvatarService avatarService, T contribuable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, contribuable);
		this.assujettissementService = assujettissementService;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		fillTiersActif(data);
		fillIdeData(data);
	}

	private void fillTiersActif(TiersIndexableData data) {
		final ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
		final boolean isActif;
		if (ffp != null) {
			isActif = ffp.isValidAt(null);
			if (ffp instanceof ForFiscalPrincipalPP) {
				data.setModeImposition(IndexerFormatHelper.enumToString(((ForFiscalPrincipalPP) ffp).getModeImposition()));
			}
		}
		else {
			isActif = false;
		}
		data.setTiersActif(isActif);
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

		// Dernier for principal
		String communeDernierFfp = null;
		RegDate dateOuvertureFor = null;
		RegDate dateFermetureFor = null;
		MotifFor motifFermetureDernierFor = null;
		final ForFiscalPrincipal dernierPrincipal = tiers.getDernierForFiscalPrincipal();
		if (dernierPrincipal != null) {
			communeDernierFfp = getLocalisationAsString(dernierPrincipal, tiers);
			dateOuvertureFor = dernierPrincipal.getDateDebut();
			dateFermetureFor = dernierPrincipal.getDateFin();
			motifFermetureDernierFor = dernierPrincipal.getMotifFermeture();
		}

		// Autre fors
		StringBuilder noOfsAutresFors = new StringBuilder();
		final Set<ForFiscal> fors = tiers.getForsFiscaux();
		if (fors != null) {
			for (ForFiscal forF : fors) {
				addValue(noOfsAutresFors, forF.getNumeroOfsAutoriteFiscale().toString());
			}
		}

		// [SIFISC-17806] on veut trouver les fors vaudois correspondant à la dernière période d'assujettissement continue
		RegDate dateDebutForVaudois;
		RegDate dateFinForVaudois;
		try {
			final List<Assujettissement> assujettissements = assujettissementService.determine(tiers);
			final List<DateRange> continuums = DateRangeHelper.merge(assujettissements);
			if (continuums != null && !continuums.isEmpty()) {
				final DateRange dernierePeriodeAssujettissementContinu = CollectionsUtils.getLastElement(continuums);

				// on va construire une collection de tous les fors vaudois qui intersectent cette période
				final List<ForFiscal> forsNonAnnules = AnnulableHelper.sansElementsAnnules(fors);
				final List<ForFiscal> forsVaudoisInteressants = new ArrayList<>(forsNonAnnules.size());
				for (ForFiscal forNonAnnule : forsNonAnnules) {
					if (forNonAnnule.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && DateRangeHelper.intersect(forNonAnnule, dernierePeriodeAssujettissementContinu)) {
						forsVaudoisInteressants.add(forNonAnnule);
					}
				}

				// ce sont la première date de début et la dernière date de fin de ces fors qui m'intéressent
				if (!forsVaudoisInteressants.isEmpty()) {
					dateDebutForVaudois = RegDateHelper.getLateDate();
					dateFinForVaudois = RegDateHelper.getEarlyDate();
					for (ForFiscal forVaudois : forsVaudoisInteressants) {
						dateDebutForVaudois = RegDateHelper.minimum(dateDebutForVaudois, forVaudois.getDateDebut(), NullDateBehavior.EARLIEST);
						dateFinForVaudois = RegDateHelper.maximum(dateFinForVaudois, forVaudois.getDateFin(), NullDateBehavior.LATEST);
					}
				}
				else {
					dateDebutForVaudois = null;
					dateFinForVaudois = null;
				}

				data.setDateOuvertureForVd(IndexerFormatHelper.dateToString(dateDebutForVaudois, IndexerFormatHelper.DateStringMode.STORAGE));
				data.setDateFermetureForVd(IndexerFormatHelper.dateToString(dateFinForVaudois, IndexerFormatHelper.DateStringMode.STORAGE));
			}
		}
		catch (AssujettissementException e) {
			LOGGER.warn("Impossible de calculer l'assujettissement du tiers " + tiers.getNumero(), e);
		}

		data.setNoOfsForPrincipal(noOfsFfpActif);
		data.setTypeOfsForPrincipal(typeAutFfpActif);
		data.setNosOfsAutresFors(noOfsAutresFors.toString());
		data.setForPrincipal(communeDernierFfp);
		data.setDateOuvertureFor(IndexerFormatHelper.dateToString(dateOuvertureFor, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setDateFermetureFor(IndexerFormatHelper.dateToString(dateFermetureFor, IndexerFormatHelper.DateStringMode.STORAGE));
		data.setMotifFermetureDernierForPrincipal(motifFermetureDernierFor);
	}
}


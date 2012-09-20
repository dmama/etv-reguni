package ch.vd.uniregctb.xml.party.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.CommonHouseholdStatus;
import ch.vd.unireg.xml.party.v1.PartyPart;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.PartyBuilder;

public class CommonHouseholdStrategy extends TaxPayerStrategy<CommonHousehold> {

	@Override
	public CommonHousehold newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final CommonHousehold menage = new CommonHousehold();
		initBase(menage, right, context);
		initParts(menage, right, parts, context);
		return menage;
	}

	@Override
	public CommonHousehold clone(CommonHousehold right, @Nullable Set<PartyPart> parts) {
		final CommonHousehold menage = new CommonHousehold();
		copyBase(menage, right);
		copyParts(menage, right, parts, CopyMode.EXCLUSIVE);
		return menage;
	}

	@Override
	protected void initParts(CommonHousehold to, ch.vd.uniregctb.tiers.Tiers from, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) from;
		if (parts != null && parts.contains(PartyPart.HOUSEHOLD_MEMBERS)) {
			initMembers(to, menage, context);
		}
	}

	@Override
	protected void copyParts(CommonHousehold to, CommonHousehold from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.HOUSEHOLD_MEMBERS)) {
			to.setMainTaxpayer(from.getMainTaxpayer());
			to.setSecondaryTaxpayer(from.getSecondaryTaxpayer());
		}
	}

	private static void initMembers(CommonHousehold left, ch.vd.uniregctb.tiers.MenageCommun menageCommun, Context context) throws ServiceException {
		EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);
		final ch.vd.uniregctb.tiers.PersonnePhysique principal = ensemble.getPrincipal();
		if (principal != null) {
			left.setMainTaxpayer(PartyBuilder.newNaturalPerson(principal, null, context));
		}

		final ch.vd.uniregctb.tiers.PersonnePhysique conjoint = ensemble.getConjoint();
		if (conjoint != null) {
			left.setSecondaryTaxpayer(PartyBuilder.newNaturalPerson(conjoint, null, context));
		}

		left.setStatus(initStatus(ensemble, context)); // [SIFISC-6028]
	}

	/**
	 * [SIFISC-6028] Détermine le status du ménage commun.
	 *
	 * @param ensemble l'ensemble tiers-couple du ménage-commun.
	 * @param context  le context d'exécution
	 * @return le statut du ménage
	 */
	private static CommonHouseholdStatus initStatus(EnsembleTiersCouple ensemble, Context context) {

		final MenageCommun menageCommun = ensemble.getMenage();

		final Set<RapportEntreTiers> rapports = menageCommun.getRapportsObjet();
		if (rapports == null || rapports.isEmpty()) {
			// il n'y a pas de relation du tout, le ménage est nul
			return null;
		}

		AppartenanceMenage derniereAppartenance = null;
		for (RapportEntreTiers rapport : rapports) {
			if (!rapport.isAnnule() && rapport instanceof AppartenanceMenage) {
				if (derniereAppartenance == null || RegDateHelper.isBefore(rapport.getDateDebut(), derniereAppartenance.getDateDebut(), NullDateBehavior.EARLIEST)) {
					derniereAppartenance = (AppartenanceMenage) rapport;
				}
			}
		}

		if (derniereAppartenance == null) {
			// il n'y a pas d'appartenance non-annulée, le ménage est nul
			return null;
		}

		final RegDate dateFermeture = derniereAppartenance.getDateFin();
		if (dateFermeture == null) {
			// la dernière appartenance est toujours en cours, le ménage est actif
			return CommonHouseholdStatus.ACTIVE;
		}

		final RegDate dateDecesPrincipal = context.tiersService.getDateDeces(ensemble.getPrincipal());
		if (dateDecesPrincipal == dateFermeture) {
			// le ménage est terminé, mais en raison du décès du principal.
			return CommonHouseholdStatus.ENDED_BY_DEATH;
		}

		final RegDate dateDecesConjoint = context.tiersService.getDateDeces(ensemble.getConjoint());
		if (dateDecesConjoint == dateFermeture) {
			// le ménage est terminé, mais en raison du décès du conjoint
			return CommonHouseholdStatus.ENDED_BY_DEATH;
		}

		// dans tous les autres cas, il s'agit d'une séparation/divorce normal
		return CommonHouseholdStatus.SEPARATED_DIVORCED;
	}
}

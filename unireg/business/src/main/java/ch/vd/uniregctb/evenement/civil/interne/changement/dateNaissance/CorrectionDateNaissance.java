package ch.vd.uniregctb.evenement.civil.interne.changement.dateNaissance;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

public class CorrectionDateNaissance extends ChangementBase {

	protected CorrectionDateNaissance(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public CorrectionDateNaissance(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	public CorrectionDateNaissance(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	public RegDate getDateNaissance() {
		return this.getDate();
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		Audit.info(getNumeroEvenement(), String.format("Correction de la date de naissance de l'individu : %d", getNoIndividu()));

		try {
			final PersonnePhysique pp = getPrincipalPP();
			if (pp != null) {
				final RegDate dateNaissance = getDate();

				// [UNIREG-1114] La date de naissance est cachée au niveau de l'habitant
				pp.setDateNaissance(dateNaissance);

				// Vérifier l'existence d'un For principal avec motif d'ouverture MAJORITE
				final ForFiscalPrincipal ffp = findForFiscalPrincipalMajorite(pp);
				if (ffp != null && !ffp.isAnnule()) {
					// Ajout de 18 ans pour atteindre la majorité
					final RegDate ancienneDateMajorite = ffp.getDateDebut();
					final RegDate nouvelleDateMajorite = dateNaissance.addYears(FiscalDateHelper.AGE_MAJORITE);

					// Lève une erreur si la nouvelle date de majorité ne tombe pas sur la même année que l'ancienne (il y a certainement des
					// DIs et d'autres choses à mettre-à-jour)
					if (ancienneDateMajorite.year() != nouvelleDateMajorite.year() && !pp.getDeclarations().isEmpty()) {
						throw new EvenementCivilException("L'ancienne (" + RegDateHelper.dateToDisplayString(ancienneDateMajorite)
								+ ") et la nouvelle date de majorité (" + RegDateHelper.dateToDisplayString(nouvelleDateMajorite)
								+ ") ne tombent pas sur la même année. Veuillez vérifier les DIs.");
					}

					ffp.setDateDebut(nouvelleDateMajorite);
				}
			}
		}
		finally {
			// forcer la reindexation du tiers
			super.handle(warnings);
		}
		return HandleStatus.TRAITE;
	}

	private ForFiscalPrincipal findForFiscalPrincipalMajorite(PersonnePhysique habitant) {

		Set<ForFiscal> fors = habitant.getForsFiscaux();
		for (ForFiscal forFiscal : fors) {
			if (forFiscal.isPrincipal() && ((ForFiscalPrincipal) forFiscal).getMotifOuverture() == MotifFor.MAJORITE) {
				return (ForFiscalPrincipal) forFiscal;
			}
		}
		return null;
	}
}

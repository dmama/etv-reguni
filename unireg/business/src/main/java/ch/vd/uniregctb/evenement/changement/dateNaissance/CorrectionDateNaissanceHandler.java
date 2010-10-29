package ch.vd.uniregctb.evenement.changement.dateNaissance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.changement.AbstractChangementHandler;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class CorrectionDateNaissanceHandler extends AbstractChangementHandler {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		Audit.info(evenement.getNumeroEvenement(), String.format("Correction de la date de naissance de l'individu : %d", evenement.getNoIndividu()));

		try {
			final List<EvenementCivilErreur> errors = new ArrayList<EvenementCivilErreur>();
			final PersonnePhysique pp = getPersonnePhysiqueOrFillErrors(evenement.getNoIndividu(), errors);
			if (pp != null) {
				final RegDate dateNaissance = evenement.getDate();

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
						throw new EvenementCivilHandlerException("L'ancienne (" + RegDateHelper.dateToDisplayString(ancienneDateMajorite)
								+ ") et la nouvelle date de majorité (" + RegDateHelper.dateToDisplayString(nouvelleDateMajorite)
								+ ") ne tombent pas sur la même année. Veuillez vérifier les DIs.");
					}

					ffp.setDateDebut(nouvelleDateMajorite);
				}
			}
		}
		finally {
			// forcer la reindexation du tiers
			super.handle(evenement, warnings);
		}
		return null;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new CorrectionDateNaissanceAdapter();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CORREC_DATE_NAISSANCE);
		return types;
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

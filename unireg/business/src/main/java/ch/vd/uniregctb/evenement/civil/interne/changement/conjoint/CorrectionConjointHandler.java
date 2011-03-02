package ch.vd.uniregctb.evenement.civil.interne.changement.conjoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class CorrectionConjointHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> errors, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> errors, List<EvenementCivilExterneErreur> warnings) {
		
		final RegDate date = evenement.getDate();
		
		final Individu individu = evenement.getIndividu();
		
		// obtention du tiers correspondant a l'individu.
		final PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), errors);
		if (habitant == null) {
			return;
		}


		final Individu individuConjoint = getServiceCivil().getConjoint(individu.getNoTechnique(), evenement.getDate());
		if (individuConjoint == null) {
			errors.add(new EvenementCivilExterneErreur(String.format("L'individu n'a pas de conjoint en date du %s dans le civil", RegDateHelper.dateToDisplayString(evenement.getDate()))));
			return;
		}
		final PersonnePhysique conjoint = getPersonnePhysiqueOrFillErrors(individuConjoint.getNoTechnique(), errors);
		
		final EnsembleTiersCouple coupleHabitant = getService().getEnsembleTiersCouple(habitant, null);
		final EnsembleTiersCouple coupleConjoint = getService().getEnsembleTiersCouple(conjoint, null);
		
		if (coupleHabitant != null && coupleConjoint != null) {
			ValidationResults validationResults = getMetier().validateFusion(coupleHabitant.getMenage(), coupleConjoint.getMenage());
			addValidationResults(errors, warnings, validationResults);
		}
		else if (coupleHabitant != null) {
			if (coupleHabitant.estComposeDe(habitant, conjoint)) {
				errors.add(new EvenementCivilExterneErreur("Les deux tiers appartiennent déjà au même ménage commun"));
			}
			else {
				ValidationResults validationResults = getMetier().validateReconstitution(coupleHabitant.getMenage(), conjoint, date);
				addValidationResults(errors, warnings, validationResults);
			}
		}
		else if (coupleConjoint != null) {
			if (coupleConjoint.estComposeDe(habitant, conjoint)) {
				errors.add(new EvenementCivilExterneErreur("Les deux tiers appartiennent déjà au même ménage commun"));
			}
			else {
				ValidationResults validationResults = getMetier().validateReconstitution(coupleConjoint.getMenage(), habitant, date);
				addValidationResults(errors, warnings, validationResults);
			}
		}
		else {
			errors.add(new EvenementCivilExterneErreur("Aucun des tiers n'appartient à un ménage commun"));
		}
		
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		final Individu individu = evenement.getIndividu();
		final PersonnePhysique habitant = getService().getPersonnePhysiqueByNumeroIndividu(individu.getNoTechnique());

		final Individu individuConjoint = getServiceCivil().getConjoint(individu.getNoTechnique(), evenement.getDate());
		final PersonnePhysique conjoint = getService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
		
		final EnsembleTiersCouple coupleHabitant = getService().getEnsembleTiersCouple(habitant, evenement.getDate());
		final EnsembleTiersCouple coupleConjoint = getService().getEnsembleTiersCouple(conjoint, evenement.getDate());
		
		final boolean memeSexe = getService().isMemeSexe(habitant, conjoint);
		final EtatCivil etatCivilFamille = memeSexe ? EtatCivil.LIE_PARTENARIAT_ENREGISTRE : EtatCivil.MARIE;
			
		if (coupleHabitant != null && coupleConjoint != null) {
			
			// reconstitution d'un ménage à partir de deux ménages communs incomplets
			handleFusionMenages(coupleHabitant, coupleConjoint, etatCivilFamille, warnings);
		}
		else if (coupleHabitant != null || coupleConjoint != null) {
			
			// reconstitution d'un ménage commun incomplet
			final ReconstitutionMenage reconstitution = getReconstitution(habitant, conjoint, coupleHabitant, coupleConjoint);
			handleReconstitutionMenage(reconstitution, etatCivilFamille, warnings);
		}

		return null;
	}

	private void handleReconstitutionMenage(final ReconstitutionMenage reconstitution, final EtatCivil etatCivilFamille, List<EvenementCivilExterneErreur> warnings) {
		
		getMetier().reconstitueMenage(reconstitution.getMenageCommun(), reconstitution.getTiers(), reconstitution.getDate(), null, etatCivilFamille);
		addCommonWarnings(warnings);
		
	}

	private void handleFusionMenages(final EnsembleTiersCouple coupleHabitant, final EnsembleTiersCouple coupleConjoint, final EtatCivil etatCivilFamille, List<EvenementCivilExterneErreur> warnings) {
		
		getMetier().fusionneMenages(coupleHabitant.getMenage(), coupleConjoint.getMenage(), null, etatCivilFamille);
		addCommonWarnings(warnings);
	}

	private void addCommonWarnings(List<EvenementCivilExterneErreur> warnings) {
		warnings.add(new EvenementCivilExterneErreur("Veuillez vérifier le mode d'imposition du contribuable ménage", TypeEvenementErreur.WARNING));
		warnings.add(new EvenementCivilExterneErreur("Veuillez vérifier la commune du for principal pour le contribuable ménage", TypeEvenementErreur.WARNING));
	}

	private ReconstitutionMenage getReconstitution(PersonnePhysique pp1, PersonnePhysique pp2, EnsembleTiersCouple couple1, EnsembleTiersCouple couple2) {
		
		final PersonnePhysique pp;
		final MenageCommun menage;
		final RegDate date;
		if (couple1 != null) {
			menage = couple1.getMenage();
			pp = pp2;
			date = pp1.getPremierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE, menage).getDateDebut();
		}
		else {
			menage = couple2.getMenage();
			pp = pp1;
			date = pp2.getPremierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE, menage).getDateDebut();
		}
		
		return new ReconstitutionMenage(menage, pp, date);
	}
	
	private static final class ReconstitutionMenage {
		
		private final MenageCommun menageCommun;
		private final PersonnePhysique tiers;
		private final RegDate date;
		
		public ReconstitutionMenage(final MenageCommun mc, final PersonnePhysique pp, final RegDate date) {
			this.menageCommun = mc;
			this.tiers = pp;
			this.date = date;
		}
		
		public MenageCommun getMenageCommun() {
			return menageCommun;
		}

		public PersonnePhysique getTiers() {
			return tiers;
		}

		public RegDate getDate() {
			return date;
		}

	}
	
	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CORREC_CONJOINT);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new CorrectionConjointAdapter(event, context, this);
	}

}
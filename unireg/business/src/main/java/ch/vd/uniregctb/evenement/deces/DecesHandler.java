package ch.vd.uniregctb.evenement.deces;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class DecesHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Rien a faire ici, un seul événement unitaire pour un déces.
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Aucune validation spécifique

		/*
		 * Cast en Deces.
		 */
		Deces deces = (Deces) target;

		/*
		 * Obtention du tiers correspondant au defunt.
		 */
		PersonnePhysique defunt = getService().getPersonnePhysiqueByNumeroIndividu(deces.getNoIndividu());

		/*
		 * Deux cas de figure :
		 * - il y a un conjoint survivant (conjoint fiscalement parlant)
		 * - il n'y a pas de conjoint survivant (conjoint fiscalement parlant)
		 */
		if (deces.getConjointSurvivant() != null) {

			/*
			 * Obtention du tiers correspondant au veuf.
			 */
			PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu( deces.getConjointSurvivant().getNoTechnique() );

			/*
			 * Récupération de l'ensemble decede-veuf-menageCommun
			 */
			EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(defunt, deces.getDate());

			/*
			 * Vérification de la cohérence
			 */
			if (menageComplet == null) {
				throw new EvenementCivilHandlerException("L'individu est marié ou en partenariat enregistré mais ne possède pas de ménage commun");
			}
			if (!menageComplet.estComposeDe(defunt, veuf)) {
				throw new EvenementCivilHandlerException(
						"Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}

			/*
			 * On récupère le tiers MenageCommun
			 */
			MenageCommun menage = menageComplet.getMenage();

			/*
			 * Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
			 */
			if (menage == null) {
				throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
			}
		}

		/*
		 * Validations métier
		 */
		ValidationResults validationResults = getMetier().validateDeces(defunt, deces.getDate());
		addValidationResults(erreurs, warnings, validationResults);
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		/*
		 * Cast en Deces.
		 */
		final Deces deces = (Deces) evenement;

		/*
		 * Obtention du tiers correspondant au defunt.
		 */
		final PersonnePhysique defunt = getService().getPersonnePhysiqueByNumeroIndividu(deces.getNoIndividu());

		// [UNIREG-775]
		final RegDate dateDecesUnireg = defunt.getDateDeces();
		if (dateDecesUnireg != null) { // si le décès a déjà été effectué dans UNIREG
			
			if (dateDecesUnireg.equals(deces.getDate())) {
				// si l'evt civil de Décès est identique à la date de décès dans UNIREG : OK (evt traité sans modif dans UNIREG)
				return null;
			}
			else {
				
				final boolean unJourDifference = RegDateHelper.isBetween(deces.getDate(), dateDecesUnireg.getOneDayBefore(), dateDecesUnireg.getOneDayAfter(), NullDateBehavior.EARLIEST);
				if (unJourDifference && dateDecesUnireg.year() == deces.getDate().year()) {
					// si 1 jour de différence dans la même Période Fiscale (même année) : OK (evt traité sans modif dans UNIREG)
					return null;
				}
				else if (!unJourDifference || dateDecesUnireg.year() != deces.getDate().year()) {
					// si plus d'1 jour d'écart ou sur une PF différente : KO (evt en Erreur --> pour traitement par la Cellule vérif de la date de décès)
					throw new EvenementCivilHandlerException("La date de décès diffère de celle dans le fiscal");
				}
			}
		}
		
		getMetier().deces(defunt, deces.getDate(), null, deces.getNumeroEvenement());
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.DECES);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new DecesAdapter();
	}

}

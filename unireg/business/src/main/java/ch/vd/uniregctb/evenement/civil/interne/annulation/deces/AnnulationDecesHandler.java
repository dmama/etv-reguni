package ch.vd.uniregctb.evenement.civil.interne.annulation.deces;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier des événements annulation décès.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationDecesHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		AnnulationDeces annulation = (AnnulationDeces) target;


		/*
		 * Obtention du tiers correspondant à l'ancient defunt.
		 */
		PersonnePhysique defunt = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getNoIndividu());

		/*
		 * Deux cas de figure :
		 * - il y a un conjoint survivant (conjoint fiscalement parlant)
		 * - il n'y a pas de conjoint survivant (conjoint fiscalement parlant)
		 */
		if (annulation.getConjointSurvivant() != null) {

			/*
			 * Obtention du tiers correspondant au veuf.
			 */
			PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu( annulation.getConjointSurvivant().getNoTechnique() );

			/*
			 * Récupération de l'ensemble decede-veuf-menageCommun
			 */
			EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(defunt, annulation.getDate());

			/*
			 * Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
			 */
			if (menageComplet == null || menageComplet.getMenage() == null) {
				throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
			}

			/*
			 * Vérification de la cohérence
			 */
			if (!menageComplet.estComposeDe(defunt, veuf)) {
				throw new EvenementCivilHandlerException(
						"Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		/*
		 * Cast en Deces.
		 */
		AnnulationDeces annulation = (AnnulationDeces) evenement;

		/*
		 * Obtention du tiers correspondant a l'ancient defunt.
		 */
		PersonnePhysique defunt = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getNoIndividu());

		getMetier().annuleDeces(defunt, annulation.getDate());
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_DECES);
		return types;
	}


	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new AnnulationDecesAdapter(event, context, this);
	}
}

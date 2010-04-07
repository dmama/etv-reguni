package ch.vd.uniregctb.evenement.tutelle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier des événements de levée de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class LeveeTutelleHandler extends AbstractTutelleHandler {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		
		LeveeTutelle leveeTutelle = (LeveeTutelle) evenement;
		/*
		 * Récupération du tiers habitant correspondant au pupille
		 */
		long numeroIndividu = leveeTutelle.getIndividu().getNoTechnique();
		PersonnePhysique pupille = getHabitantOrThrowException(numeroIndividu);
		
		RapportEntreTiers rapportEntreTiers = getRapportTutelleOuvert(pupille, leveeTutelle.getDate());
		if (rapportEntreTiers == null) {
			throw new EvenementCivilHandlerException("L'individu " + numeroIndividu + " n'a aucun rapport de type tutelle");
		}
		
		// Clôture de la tutelle
		rapportEntreTiers.setDateFin(leveeTutelle.getDate());
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.LEVEE_TUTELLE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new LeveeTutelleAdapter();
	}

}

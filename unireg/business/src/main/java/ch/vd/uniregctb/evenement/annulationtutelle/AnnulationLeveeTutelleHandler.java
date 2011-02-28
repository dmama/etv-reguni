package ch.vd.uniregctb.evenement.annulationtutelle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.tutelle.AbstractTutelleHandler;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier des événements d'annulation de levée de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationLeveeTutelleHandler extends AbstractTutelleHandler {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		
		AnnulationLeveeTutelle annulationLeveeTutelle = (AnnulationLeveeTutelle) evenement;
		// Récupération du tiers habitant correspondant au pupille
		long numeroIndividu = annulationLeveeTutelle.getNoIndividu();
		PersonnePhysique pupille = getPersonnePhysiqueOrThrowException(numeroIndividu);

		// Récupération du rapport entre tiers (de type tutelle)
		RapportEntreTiers rapportEntreTiers = getRapportTutelleOuvert(pupille, annulationLeveeTutelle.getDate());
		if (rapportEntreTiers == null) {
			throw new EvenementCivilHandlerException("L'individu " + numeroIndividu + " n'a aucun rapport de type tutelle");
		}
		
		// Remise à null de la date de fin de tutelle
		rapportEntreTiers.setDateFin(null);
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_LEVEE_TUTELLE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException {
		return new AnnulationLeveeTutelleAdapter(event, context);
	}

}
package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.AbstractTutelleHandler;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier des événements d'annulation de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationTutelleHandler extends AbstractTutelleHandler {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		
		AnnulationTutelle annulationTutelle = (AnnulationTutelle) evenement;
		// Récupération du tiers habitant correspondant au pupille
		long numeroIndividu = annulationTutelle.getNoIndividu();
		PersonnePhysique pupille = getPersonnePhysiqueOrThrowException(numeroIndividu);
		
		// Récupération du rapport entre tiers (de type tutelle)
		RapportEntreTiers rapportEntreTiers = getRapportTutelleOuvert(pupille, annulationTutelle.getDate());
		if (rapportEntreTiers == null) {
			throw new EvenementCivilHandlerException("L'individu " + numeroIndividu + " n'a aucun rapport de type tutelle");
		}
		
		// Annulation du rapport
		rapportEntreTiers.setAnnule(true);
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_MESURE_TUTELLE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new AnnulationTutelleAdapter(event, context, this);
	}

}

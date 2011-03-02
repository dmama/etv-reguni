package ch.vd.uniregctb.evenement.civil.interne.annulation.veuvage;

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
 * Traitements métier pour événements d'annulation de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationVeuvageHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		
		AnnulationVeuvage annulation = (AnnulationVeuvage) target;
		
		long numeroIndividu = annulation.getNoIndividu();
		PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
		
		/*
		 * Récupération du ménage du veuf
		 */
		EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(veuf, annulation.getDate().getOneDayAfter());
		
		if (menageComplet != null || (menageComplet != null && (menageComplet.getMenage() != null || menageComplet.getConjoint(veuf) != null))) {
			/*
			 * Normalement l'événement de veuvage ne doit s'appliquer aux personnes encores mariées (seules).
			 */
			erreurs.add(new EvenementCivilExterneErreur("L'événement d'annulation veuvage ne peut pas s'appliquer à une personne mariée."));
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		/*
		 * Annulation de veuvage
		 */
		AnnulationVeuvage annulation = (AnnulationVeuvage) evenement;
		
		/*
		 * Obtention du tiers
		 */
		PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getNoIndividu());
		
		/*
		 * Traitement de l'événement
		 */
		getMetier().annuleVeuvage(veuf, annulation.getDate(), annulation.getNumeroEvenement());
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_VEUVAGE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new AnnulationVeuvageAdapter(event, context, this);
	}
}

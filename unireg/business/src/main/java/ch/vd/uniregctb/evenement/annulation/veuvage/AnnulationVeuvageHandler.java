package ch.vd.uniregctb.evenement.annulation.veuvage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier pour événements d'annulation de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationVeuvageHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		
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
			erreurs.add(new EvenementCivilErreur("L'événement d'annulation veuvage ne peut pas s'appliquer à une personne mariée."));
		}
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
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
	public GenericEvenementAdapter createAdapter() {
		return new AnnulationVeuvageAdapter();
	}
}

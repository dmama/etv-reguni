package ch.vd.uniregctb.evenement.annulationpermis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Règles métiers permettant de traiter les événements d'annulation de permis C.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationPermisHandler extends AnnulationPermisCOuNationaliteSuisseHandler {

	@Override
	public void checkCompleteness(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// rien à faire
	}

	@Override
	protected void validateSpecific(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// rien à faire
	}
	
	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.annulationpermis.AnnulationPermisCOuNationaliteSuisseHandler#handle(ch.vd.uniregctb.evenement.EvenementCivil, java.util.List)
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		AnnulationPermis annulationPermis = (AnnulationPermis) evenement;
		if (isAnnulationPermisC(annulationPermis)) {
			return super.handle(annulationPermis, warnings);
		}
		return null;
	}

	private boolean isAnnulationPermisC(AnnulationPermis annulationPermis) {
		return annulationPermis.getTypePermis() == TypePermis.ETABLISSEMENT;
	}
	
	@Override
	public GenericEvenementAdapter createAdapter() {
		return new AnnulationPermisAdapter();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER);
		return types;
	}

}

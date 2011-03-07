package ch.vd.uniregctb.evenement.civil.interne.annulation.depart;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Gère l'annulation de départ d'un individu dans les cas suivants:
 * <ul>
 * <li>SUPPRESSION_DEPART_COMMUNE : départ de la commune</li>
 * <li>SUPPRESSION_DEPART_SECONDAIRE : départ secondaire</li>
 * </ul>
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionDepartHandler extends EvenementCivilHandlerBase {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.SUP_DEPART_COMMUNE);
		types.add(TypeEvenementCivil.SUP_DEPART_SECONDAIRE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new SuppressionDepartAdapter(event, context);
	}
}

package ch.vd.uniregctb.evenement.civil.common;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.engine.EvenementHandlerRegistrar;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public interface EvenementCivilHandler {

	@Deprecated
	void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings);

	/**
	 * Crée une Adapter valide pour ce Handler
	 *
	 *
	 *
	 * @param event
	 * @param context le context d'exécution de l'événement civil
	 * @return un événement civil interne qui corresponds à l'événement civil externe reçu
	 */
	EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException;

	@Deprecated
	void validate(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings);

	@Deprecated
	Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException;

	void setRegistrar(EvenementHandlerRegistrar registrar);
}

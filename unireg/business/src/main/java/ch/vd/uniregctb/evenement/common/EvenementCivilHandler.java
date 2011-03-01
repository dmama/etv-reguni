package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.engine.EvenementHandlerRegistrar;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public interface EvenementCivilHandler {

	@Deprecated
	void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Crée une Adapter valide pour ce Handler
	 *
	 *
	 *
	 * @param event
	 * @param context le context d'exécution de l'événement civil
	 * @return un événement civil interne qui corresponds à l'événement civil externe reçu
	 */
	GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException;

	@Deprecated
	void validate(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	@Deprecated
	Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException;

	void setRegistrar(EvenementHandlerRegistrar registrar);
}

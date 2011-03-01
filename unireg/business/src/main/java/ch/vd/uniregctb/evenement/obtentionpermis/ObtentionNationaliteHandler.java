package ch.vd.uniregctb.evenement.obtentionpermis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements de démangement vaudois.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public class ObtentionNationaliteHandler extends ObtentionPermisCOuNationaliteSuisseHandler {

	/**
	 * Un logger.
	 */
	//private static final Logger LOGGER =  Logger.getLogger(ObtentionNationaliteHandler.class);

	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Obsolète dans cet handler, l'obtention de nationalité est un événement ne concernant qu'un seul individu.
	}

	/**
	 * @see ch.vd.uniregctb.evenement.obtentionpermis.ObtentionPermisCOuNationaliteSuisseHandler#validateSpecific(ch.vd.uniregctb.evenement.EvenementCivil, java.util.List, java.util.List)
	 */
	@Override
	public void validateSpecific(EvenementCivil evenementCivil, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		final ObtentionNationalite obtentionNationalite = (ObtentionNationalite) evenementCivil;

		if (TypeEvenementCivil.NATIONALITE_SUISSE == obtentionNationalite.getType()) {
			super.validateSpecific(obtentionNationalite, erreurs, warnings);
		}
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		final ObtentionNationalite obtentionNationalite = (ObtentionNationalite) evenement;

		// quelque soit la nationalité, si l'individu correspond à un non-habitant (= ancien habitant)
		// il faut mettre à jour la nationalité chez nous
		final PersonnePhysique pp = getService().getPersonnePhysiqueByNumeroIndividu(evenement.getNoIndividu());
		if (pp != null && !pp.isHabitantVD()) {
			if (obtentionNationalite.getType() == TypeEvenementCivil.NATIONALITE_SUISSE) {
				pp.setNumeroOfsNationalite(ServiceInfrastructureService.noOfsSuisse);
			}
			else {
				for (Nationalite nationalite : evenement.getIndividu().getNationalites()) {
					if (evenement.getDate().equals(nationalite.getDateDebutValidite())) {
						pp.setNumeroOfsNationalite(nationalite.getPays().getNoOFS());
						Audit.info(evenement.getNumeroEvenement(), String.format("L'individu %d (tiers non-habitant %s) a maintenant la nationalité du pays '%s'",
																				evenement.getNoIndividu(), FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), nationalite.getPays().getNomMinuscule()));
						break;
					}
				}
			}
		}

		switch (obtentionNationalite.getType()) {
			case NATIONALITE_SUISSE:
				return super.handle(evenement, warnings);

			case NATIONALITE_NON_SUISSE:
				/* Seul l'obtention de nationalité suisse est traitée */
				Audit.info(obtentionNationalite.getNumeroEvenement(), "Nationalité non suisse : ignorée fiscalement");
				break;

			default:
				Assert.fail();
		}

		return null;
	}

	@Override
	protected Set<TypeEvenementCivil>  getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.NATIONALITE_NON_SUISSE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException {
		return new ObtentionNationaliteAdapter(event, context, this);
	}

}

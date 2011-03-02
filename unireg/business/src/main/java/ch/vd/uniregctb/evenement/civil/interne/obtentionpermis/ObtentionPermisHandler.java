package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Règles métiers permettant de traiter les événements d'obtention de permis.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public class ObtentionPermisHandler extends ObtentionPermisCOuNationaliteSuisseHandler {

	//private static final Logger LOGGER =  Logger.getLogger(ObtentionPermisHandler.class);

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// Obsolète dans cet handler, l'obtention de permis est un événement ne concernant qu'un seul individu.
	}

	/**
	 * Traite l'événement passé en paramètre.
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		final ObtentionPermis obtentionPermis = (ObtentionPermis) evenement;

		// quelque soit le permis, si l'individu correspond à un non-habitant (= ancien habitant)
		// il faut mettre à jour le permis chez nous
		final PersonnePhysique pp = getService().getPersonnePhysiqueByNumeroIndividu(evenement.getNoIndividu());
		if (pp != null && !pp.isHabitantVD()) {
			pp.setCategorieEtranger(CategorieEtranger.enumToCategorie(obtentionPermis.getTypePermis()));
			Audit.info(evenement.getNumeroEvenement(), String.format("L'individu %d (tiers non-habitant %s) a maintenant le permis '%s'",
																	evenement.getNoIndividu(), FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), obtentionPermis.getTypePermis().name()));
		}

		/* Seul le permis C a une influence */
		if (obtentionPermis.getTypePermis() != TypePermis.ETABLISSEMENT) {
			Audit.info(obtentionPermis.getNumeroEvenement(), "Permis non C : ignoré fiscalement");
			return null;
		}
		else {
			return super.handle(evenement, warnings);
		}
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new ObtentionPermisAdapter(event, context, this);
	}

}

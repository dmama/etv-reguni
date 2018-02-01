package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Règles métiers permettant de traiter les événements d'annulation de permis
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationPermisTranslationStrategy extends AnnulationPermisOuNationaliteTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationPermis(event, context, options);
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationPermis(event, context, options, getTypePermisAnnule(event, context));
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return !isPermisC(event, context);
	}
	
	private static boolean isPermisC(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return getTypePermisAnnule(event, context) == TypePermis.ETABLISSEMENT;
	}

	private static TypePermis getTypePermisAnnule(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		final Individu individu = getIndividuAvant(event, context);
		final PermisList permisList = individu.getPermis();
		final Permis permis = permisList == null ? null : permisList.getPermisActif(event.getDateEvenement());
		if (permis == null) {
			throw new EvenementCivilException(
					String.format("Aucun permis connu pour l'individu %d avant annulation en date du %s", event.getNumeroIndividu(), RegDateHelper.dateToDisplayString(event.getDateEvenement())));
		}
		return permis.getTypePermis();
	}
}

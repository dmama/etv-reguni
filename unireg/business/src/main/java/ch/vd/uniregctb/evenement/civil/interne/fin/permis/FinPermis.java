package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Adapter pour la fin obtention d'un permis.
 *
 * @author Pavel BLANCO
 *
 */
public class FinPermis extends EvenementCivilInterne {

	private final static Logger LOGGER = Logger.getLogger(FinPermis.class);

	private TypePermis typePermis;

	protected FinPermis(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		try {
			// on récupère le permis à partir de sa date de fin (= à la date d'événement)
			final Permis permis = context.getServiceCivil().getPermis(super.getNoIndividu(), evenement.getDateEvenement());
			if (permis == null || permis.getDateFin() != evenement.getDateEvenement()) {
				throw new EvenementCivilException("Le permis n'a pas été trouvé dans le registre civil");
			}
			this.typePermis = permis.getTypePermis();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EvenementCivilException(e.getMessage(), e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected FinPermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, TypePermis typePermis, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER, date, numeroOfsCommuneAnnonce, context);
		this.typePermis = typePermis;
	}

	public TypePermis getTypePermis() {
		return typePermis;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		/* Seulement le permis C est traité */
		if (getTypePermis() == TypePermis.ETABLISSEMENT) {

			PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(getNoIndividu(), erreurs);
			if (habitant == null) {
				return;
			}

			boolean isSuisse = false;
			// vérification si la nationalité suisse a été obtenue
			try {
				isSuisse = context.getTiersService().isSuisse(habitant, getDate().getOneDayAfter());
			}
			catch (Exception e) {
				erreurs.add(new EvenementCivilExterneErreur(e.getMessage()));
				return;
			}

			if (isSuisse) {
				Audit.info(getNumeroEvenement(), "Permis C : l'habitant a obtenu la nationalité suisse, rien à faire");
			}
			else {
				Audit.info(getNumeroEvenement(), "Permis C : l'habitant n'a pas obtenu la nationalité suisse, passage en traitement manuel");
				erreurs.add(new EvenementCivilExterneErreur("La fin du permis C doit être traitée manuellement"));
			}
		}
		else {
			Audit.info(getNumeroEvenement(), "Permis non C : ignoré");
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		// rien à faire tout ce passe dans le validateSpecific
		return null;
	}
}

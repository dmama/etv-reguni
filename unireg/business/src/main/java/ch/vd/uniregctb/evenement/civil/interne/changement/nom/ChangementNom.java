package ch.vd.uniregctb.evenement.civil.interne.changement.nom;

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
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de changement de nom.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class ChangementNom extends ChangementBase {

	protected static Logger LOGGER = Logger.getLogger(ChangementNom.class);

	protected ChangementNom(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ChangementNom(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		/* pas de validation spécifique pour le changement de nom */
		/* l'existance de l'individu est vérifié dans validateCommon */
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		final long noIndividu = getNoIndividu();

		Audit.info(getNumeroEvenement(), String.format("Changement de nom de l'individu : %d", noIndividu));

		final PersonnePhysique pp = context.getTiersDAO().getPPByNumeroIndividu(noIndividu, true);
		if (pp != null && !pp.isHabitantVD()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? nom/prénom !
			final Individu individu = context.getTiersService().getIndividu(pp);

			// nom / prénom
			final String nom = individu.getNom();
			final String prenom = individu.getPrenom();
			pp.setNom(nom != null ? nom.trim() : "");
			pp.setPrenom(prenom != null ? prenom.trim() : "");
		}

		return super.handle(warnings);
	}
}

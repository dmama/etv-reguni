package ch.vd.unireg.evenement.civil.interne.changement.nom;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.interne.changement.ChangementBase;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.tiers.PersonnePhysique;

/**
 * Modélise un événement de changement de nom.
 */
public class ChangementNom extends ChangementBase {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ChangementNom.class);

	protected ChangementNom(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public ChangementNom(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ChangementNom(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/* pas de validation spécifique pour le changement de nom */
		/* l'existance de l'individu est vérifié dans validateCommon */
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final long noIndividu = getNoIndividu();

		context.audit.info(getNumeroEvenement(), String.format("Changement de nom de l'individu : %d", noIndividu));

		final PersonnePhysique pp = getPrincipalPP();
		if (pp != null && !pp.isHabitantVD()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? nom/prénom !
			final Individu individu = context.getTiersService().getIndividu(pp);

			// nom / prénom
			final String nom = individu.getNom();
			final String prenomUsuel = individu.getPrenomUsuel();
			final String tousPrenoms = individu.getTousPrenoms();
			pp.setNom(StringUtils.trimToEmpty(nom));
			pp.setPrenomUsuel(StringUtils.trimToEmpty(prenomUsuel));
			pp.setTousPrenoms(StringUtils.trimToEmpty(tousPrenoms));
		}

		return super.handle(warnings);
	}
}

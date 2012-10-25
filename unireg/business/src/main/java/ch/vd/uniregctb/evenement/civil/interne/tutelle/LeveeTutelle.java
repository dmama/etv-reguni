package ch.vd.uniregctb.evenement.civil.interne.tutelle;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;

public class LeveeTutelle extends EvenementCivilInterne {

	protected LeveeTutelle(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected LeveeTutelle(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * Récupération du tiers habitant correspondant au pupille
		 */
		long numeroIndividu = getNoIndividu();
		PersonnePhysique pupille = getPersonnePhysiqueOrThrowException(numeroIndividu);

		RapportEntreTiers rapportEntreTiers = Tutelle.getRapportTutelleOuvert(pupille, getDate());
		if (rapportEntreTiers == null) {
			throw new EvenementCivilException("L'individu " + numeroIndividu + " n'a aucun rapport de type tutelle");
		}

		// Clôture de la tutelle
		rapportEntreTiers.setDateFin(getDate());
		return HandleStatus.TRAITE;
	}
}

package ch.vd.unireg.evenement.civil.interne.arrivee;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;

public class ArriveeSecondaire extends Arrivee {

	private final Adresse ancienneAdresse;
	private final Adresse nouvelleAdresse;
	private final Commune ancienneCommune;
	private final Commune nouvelleCommune;

	public ArriveeSecondaire(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateArrivee = getDate();
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		final AdressesCiviles anciennesAdresses = getAdresses(context, veilleArrivee);
		ancienneAdresse = anciennesAdresses.secondaireCourante;
		ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, veilleArrivee);

		final AdressesCiviles nouvellesAdresses = getAdresses(context, dateArrivee);
		nouvelleAdresse = nouvellesAdresses.secondaireCourante;
		nouvelleCommune = getCommuneByAdresse(context, nouvelleAdresse, dateArrivee);
	}

	public ArriveeSecondaire(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateArrivee = getDate();
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		final AdressesCiviles anciennesAdresses = getAdresses(context, veilleArrivee);
		ancienneAdresse = anciennesAdresses.secondaireCourante;
		ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, veilleArrivee);

		final AdressesCiviles nouvellesAdresses = getAdresses(context, dateArrivee);
		nouvelleAdresse = nouvellesAdresses.secondaireCourante;
		nouvelleCommune = getCommuneByAdresse(context, nouvelleAdresse, dateArrivee);
	}

	/**
	 * Pour les tests seulement
	 */
	@SuppressWarnings({"JavaDoc"})
	public ArriveeSecondaire(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Commune ancienneCommune, Commune nouvelleCommune, Adresse ancienneAdresse,
	                         Adresse nouvelleAdresse, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.ancienneCommune = ancienneCommune;
		this.nouvelleCommune = nouvelleCommune;
		this.ancienneAdresse = ancienneAdresse;
		this.nouvelleAdresse = nouvelleAdresse;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		super.validateSpecific(erreurs, warnings);
		validateArriveeAdresseSecondaire(erreurs);
	}

	protected void validateArriveeAdresseSecondaire(EvenementCivilErreurCollector erreurs) {
		/*
		 * La date de début de la nouvelle adresse secondaire de l’individu est antérieure ou identique à la date de l'ancienne.
		 */
		if (ancienneAdresse != null && ancienneAdresse.getDateFin() != null && getDate().isBeforeOrEqual(ancienneAdresse.getDateFin())) {
			erreurs.addErreur("La date d'arrivée secondaire est antérieure à la date de fin de l'ancienne adresse");
		}

		/*
		 * La nouvelle adresse secondaire n’est pas dans le canton (il n’est pas obligatoire que l’adresse courrier soit dans le canton).
		 */
		if (nouvelleCommune == null || !nouvelleCommune.isVaudoise()) {
			erreurs.addErreur("La nouvelle commune secondaire est en dehors du canton");
		}
	}

	@Override
	protected void doHandleCreationForIndividuSeul(PersonnePhysique habitant, EvenementCivilWarningCollector warnings) {
		// Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire
	}

	@Override
	protected void doHandleCreationForMenage(PersonnePhysique arrivant, MenageCommun menageCommun, EvenementCivilWarningCollector warnings) {
		// Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire
	}

	@Override
	protected boolean isArriveeRedondantePourIndividuSeul() {
		// comme on ne fait rien, il est impossible de dire si l'événement est effectivement redondant
		return false;
	}

	@Override
	protected boolean isArriveeRedondantePourIndividuEnMenage() {
		// comme on ne fait rien, il est impossible de dire si l'événement est effectivement redondant
		return false;
	}

	@Override
	protected boolean isArriveeRedondanteAnterieurPourIndividuEnMenage() {
		// comme on ne fait rien, il est impossible de dire si l'événement est antérieur
		return false;
	}

	@Override
	protected boolean isConjointMarieSeul() {
		// comme on ne fait rien, il est impossible de dire si l'événement est antérieur
		return false;
	}

	@Override
	protected boolean isArriveeRedondantePosterieurPourIndividuEnMenage() {
		// comme on ne fait rien, il est impossible de dire si l'événement est postérieur
		return false;
	}

}

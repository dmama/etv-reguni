package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ArriveeSecondaire extends Arrivee {

	private final Adresse ancienneAdresse;
	private final Adresse nouvelleAdresse;
	private final Commune ancienneCommune;
	private final Commune nouvelleCommune;

	public ArriveeSecondaire(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateArrivee = getDate();
		final RegDate veilleArrivee = dateArrivee.getOneDayBefore();

		final AdressesCiviles anciennesAdresses = getAdresses(context, veilleArrivee);
		ancienneAdresse = anciennesAdresses.secondaire;
		ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, veilleArrivee);

		final AdressesCiviles nouvellesAdresses = getAdresses(context, dateArrivee);
		nouvelleAdresse = nouvellesAdresses.secondaire;
		nouvelleCommune = getCommuneByAdresse(context, nouvelleAdresse, dateArrivee);
	}

	/**
	 * Pour les tests seulement
	 */
	@SuppressWarnings({"JavaDoc"})
	public ArriveeSecondaire(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Commune ancienneCommune, Commune nouvelleCommune, Adresse ancienneAdresse,
	                         Adresse nouvelleAdresse, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.ARRIVEE_SECONDAIRE, date, numeroOfsCommuneAnnonce, context);
		this.ancienneCommune = ancienneCommune;
		this.nouvelleCommune = nouvelleCommune;
		this.ancienneAdresse = ancienneAdresse;
		this.nouvelleAdresse = nouvelleAdresse;
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		super.validateSpecific(erreurs, warnings);
		validateArriveeAdresseSecondaire(erreurs);
	}

	protected void validateArriveeAdresseSecondaire(List<EvenementCivilExterneErreur> erreurs) {
		/*
		 * La date de début de la nouvelle adresse secondaire de l’individu est antérieure ou identique à la date de l'ancienne.
		 */
		if (ancienneAdresse != null && ancienneAdresse.getDateFin() != null && getDate().isBeforeOrEqual(ancienneAdresse.getDateFin())) {
			erreurs.add(new EvenementCivilExterneErreur("La date d'arrivée secondaire est antérieure à la date de fin de l'ancienne adresse"));
		}

		/*
		 * La nouvelle adresse secondaire n’est pas dans le canton (il n’est pas obligatoire que l’adresse courrier soit dans le canton).
		 */
		if (nouvelleCommune == null || !nouvelleCommune.isVaudoise()) {
			erreurs.add(new EvenementCivilExterneErreur("La nouvelle commune secondaire est en dehors du canton"));
		}
	}

	@Override
	protected void doHandleCreationForIndividuSeul(PersonnePhysique habitant, List<EvenementCivilExterneErreur> warnings) {
		// Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire
	}

	@Override
	protected void doHandleCreationForMenage(PersonnePhysique arrivant, MenageCommun menageCommun, List<EvenementCivilExterneErreur> warnings) {
		// Le for fiscal principal reste inchangé en cas d'arrivée en résidence secondaire
	}
}

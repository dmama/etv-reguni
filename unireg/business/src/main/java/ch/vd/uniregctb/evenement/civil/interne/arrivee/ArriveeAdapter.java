package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdressesBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Modélise un événement d'arrivée.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class ArriveeAdapter extends EvenementCivilInterneAvecAdressesBase implements Arrivee {

	protected static Logger LOGGER = Logger.getLogger(ArriveeAdapter.class);

	private Adresse ancienneAdressePrincipale;
	private Adresse ancienneAdresseSecondaire;
	private CommuneSimple ancienneCommunePrincipale;
	private CommuneSimple ancienneCommuneSecondaire;
	private CommuneSimple nouvelleCommunePrincipale;
	private CommuneSimple nouvelleCommuneSecondaire;

	private ArriveeHandler handler;

	public ArriveeAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, ArriveeHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;
		Assert.isTrue(isEvenementArrivee(evenement.getType()));

		// on récupère les nouvelles adresses (= à la date d'événement)
		final RegDate veilleArrivee = evenement.getDateEvenement().getOneDayBefore();
		final AdressesCiviles anciennesAdresses;
		try {
			anciennesAdresses = new AdressesCiviles(context.getServiceCivil().getAdresses(super.getNoIndividu(), veilleArrivee, false));
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilInterneException(e);
		}

		this.ancienneAdressePrincipale = anciennesAdresses.principale;
		this.ancienneAdresseSecondaire = anciennesAdresses.secondaire;

		try {
			// on récupère les nouvelles communes
			this.nouvelleCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(getNouvelleAdressePrincipale());
			this.nouvelleCommuneSecondaire = context.getServiceInfra().getCommuneByAdresse(getNouvelleAdresseSecondaire());

			// on récupère les anciennes communes
			this.ancienneCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(ancienneAdressePrincipale);
			this.ancienneCommuneSecondaire = context.getServiceInfra().getCommuneByAdresse(ancienneAdresseSecondaire);
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilInterneException(e);
		}
	}

	public final Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public final Adresse getAncienneAdresseSecondaire() {
		return ancienneAdresseSecondaire;
	}

	public CommuneSimple getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public CommuneSimple getAncienneCommuneSecondaire() {
		return ancienneCommuneSecondaire;
	}

	public final Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale(); // par définition
	}

	public final Adresse getNouvelleAdresseSecondaire() {
		return getAdresseSecondaire(); // par définition
	}

	public final CommuneSimple getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public final CommuneSimple getNouvelleCommuneSecondaire() {
		return nouvelleCommuneSecondaire;
	}

	@Override
	public boolean isContribuablePresentBefore() {
		/*
		 * par définition, dans le case d'une arrivée le contribuable peut ne pas encore exister dans la base de données fiscale
		 */
		return false;
	}

	private boolean isEvenementArrivee(TypeEvenementCivil type) {
		boolean isPresent = false;
		if (type == TypeEvenementCivil.ARRIVEE_DANS_COMMUNE
				|| type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC
				|| type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS
				|| type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE
				|| type == TypeEvenementCivil.ARRIVEE_SECONDAIRE) {
			isPresent = true;

		}
		return isPresent;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}

}

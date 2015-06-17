package ch.vd.unireg.interfaces.organisation.rcent;

import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Country;
import ch.vd.evd0022.v1.Function;
import ch.vd.evd0022.v1.Party;
import ch.vd.evd0022.v1.Person;
import ch.vd.evd0022.v1.PlaceOfResidence;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Autorisation;
import ch.vd.unireg.interfaces.organisation.data.Fonction;
import ch.vd.unireg.interfaces.organisation.data.LieuDeResidence;
import ch.vd.unireg.interfaces.organisation.data.Partie;
import ch.vd.unireg.interfaces.organisation.data.Personne;

public class RCEntFonctionConverter {

	public static Fonction get(Function function) {
		final Party party = function.getParty();

		final Person person = party.getPerson();
		Partie partie = new Partie(person.getCantonalId(),
		                    personneFromPerson(person),
		                    RCEntAddressHelper.fromRCEntAddress(party.getAddress()),
							lieuResidenceFromPlaceOfResidence(party.getPlaceOfResidence())
		);
		String texteFonction = function.getFunctionText();
		Autorisation autorisation= null;
		if (function.getAuthorisation() != null) {
			autorisation = Autorisation.valueOf(function.getAuthorisation().toString());
		}
		String restrictionAutorisation = function.getAuthorisationRestriction();

		return new Fonction(autorisation, partie, texteFonction, restrictionAutorisation);
	}

	private static LieuDeResidence lieuResidenceFromPlaceOfResidence(PlaceOfResidence placeOfResidence) {
		final Country country = placeOfResidence.getCountry();
		return new LieuDeResidence(placeOfResidence.getPlaceOfResidenceName(),
		                           placeOfResidence.getSwissMunicipality().getMunicipalityId(),
		                           country.getCountryId()
		);
	}

	private static Personne personneFromPerson(Person person) {
		return new Personne(convertDateNaissance(person),
		                    person.getVn(),
		                    person.getName(),
		                    person.getFirstName(),
		                    Personne.Sexe.valueOf(person.getSex().toString()),
		                    Personne.SourceDonnees.valueOf(person.getPersonDataSource().toString()));
	}

	@NotNull
	private static RegDate convertDateNaissance(Person person) {
		RegDate date = person.getDateOfBirth().getYearMonthDay();
		if (date == null) {
			XMLGregorianCalendar dm = person.getDateOfBirth().getYearMonth();
			if (dm != null) {
				date = RegDate.get(dm.getYear(), dm.getMonth());
			} else {
				dm = person.getDateOfBirth().getYear();
				date = RegDate.get(dm.getYear());
			}
		}
		return date;
	}

}

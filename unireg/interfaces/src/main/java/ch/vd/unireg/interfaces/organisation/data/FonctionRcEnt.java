package ch.vd.unireg.interfaces.organisation.data;

import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Country;
import ch.vd.evd0022.v1.Function;
import ch.vd.evd0022.v1.Party;
import ch.vd.evd0022.v1.Person;
import ch.vd.evd0022.v1.PlaceOfResidence;
import ch.vd.registre.base.date.RegDate;

public class FonctionRcEnt implements Fonction {

	@NotNull
	private final Partie partie;
	private final String texteFonction;
	private final Autorisation autorisation;
	private final String restrictionAutorisation;

	public FonctionRcEnt(Function function) {
		final Party party = function.getParty();
		if (party != null) {
			final Person person = party.getPerson();
			partie = new Partie(personneFromPerson(person),
			                    RCEntAddressHelper.fromRCEntAddress(party.getAddress()),
			                    lieuResidenceFromPlaceOfResidence(party.getPlaceOfResidence()));
		} else {
			partie = null;
		}
		texteFonction = function.getFunctionText();
		if (function.getAuthorisation() != null) {
			autorisation = Autorisation.valueOf(function.getAuthorisation().toString());
		} else {
			autorisation = null;
		}
		restrictionAutorisation = function.getAuthorisationRestriction();

	}

	private LieuDeResidence lieuResidenceFromPlaceOfResidence(PlaceOfResidence placeOfResidence) {
		final Country country = placeOfResidence.getCountry();
		return new LieuDeResidence(placeOfResidence.getPlaceOfResidenceName(),
		                           placeOfResidence.getSwissMunicipality().getMunicipalityId(),
		                           new Pays(country.getCountryId(), country.getCountryIdISO2(), country.getCountryName()));
	}

	private Personne personneFromPerson(Person person) {
		return new Personne(person.getCantonalId(),
		                    convertDateNaissance(person),
		                    person.getVn(),
		                    person.getName(),
		                    person.getFirstName(),
							Personne.Sexe.valueOf(person.getSex().toString()),
		                    Personne.SourceDonnees.valueOf(person.getPersonDataSource().toString()));
	}

	@NotNull
	private RegDate convertDateNaissance(Person person) {
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

	public FonctionRcEnt(@NotNull Partie partie, String texteFonction, Autorisation autorisation, String restrictionAutorisation) {
		this.partie = partie;
		this.texteFonction = texteFonction;
		this.autorisation = autorisation;
		this.restrictionAutorisation = restrictionAutorisation;
	}

	public Autorisation getAutorisation() {
		return autorisation;
	}

	public String getRestrictionAutorisation() {
		return restrictionAutorisation;
	}

	public String getTexteFonction() {
		return texteFonction;
	}

	public Partie getPartie() {
		return partie;
	}
}

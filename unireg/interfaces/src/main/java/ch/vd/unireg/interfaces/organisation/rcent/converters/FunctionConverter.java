package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.Function;
import ch.vd.evd0022.v1.Party;
import ch.vd.evd0022.v1.Person;
import ch.vd.unireg.interfaces.organisation.data.Autorisation;
import ch.vd.unireg.interfaces.organisation.data.Fonction;
import ch.vd.unireg.interfaces.organisation.data.Partie;
import ch.vd.unireg.interfaces.organisation.data.Personne;

public class FunctionConverter extends BaseConverter<Function, Fonction> {

	private static final AddressConverter ADDRESS_CONVERTER = new AddressConverter();
	private static final PlaceOfResidenceConverter PLACE_OF_RESIDENCE_CONVERTER = new PlaceOfResidenceConverter();
	private static final AutorisationConverter AUTORISATION_CONVERTER = new AutorisationConverter();
	private static final DatePartiallyKnownConverter DATE_PARTIALLY_KNOWN_CONVERTER = new DatePartiallyKnownConverter();
	private static final SexConverter SEX_CONVERTER = new SexConverter();
	private static final PersonDataSourceConverter  PERSON_DATA_SOURCE_CONVERTER = new PersonDataSourceConverter();

	@Override
	protected Fonction convert(@NotNull Function function) {
		final Party party = function.getParty();

		Partie partie = new Partie(party.getPerson().getCantonalId(),
		                           personneFromPerson(party.getPerson()),
		                           ADDRESS_CONVERTER.apply(party.getAddress()),
		                           PLACE_OF_RESIDENCE_CONVERTER.apply(party.getPlaceOfResidence())
		);
		String texteFonction = function.getFunctionText();
		Autorisation autorisation= null;
		if (function.getAuthorisation() != null) {
			autorisation = AUTORISATION_CONVERTER.apply(function.getAuthorisation());
		}
		String restrictionAutorisation = function.getAuthorisationRestriction();

		return new Fonction(autorisation, partie, texteFonction, restrictionAutorisation);
	}

	private static Personne personneFromPerson(Person person) {
		return new Personne(DATE_PARTIALLY_KNOWN_CONVERTER.apply(person.getDateOfBirth()),
		                    person.getVn(),
		                    person.getName(),
		                    person.getFirstName(),
		                    SEX_CONVERTER.apply(person.getSex()),
		                    PERSON_DATA_SOURCE_CONVERTER.apply(person.getPersonDataSource()));
	}
}

package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.PersonDataSource;
import ch.vd.unireg.interfaces.organisation.data.Personne;

public class PersonDataSourceConverter extends BaseEnumConverter<PersonDataSource, Personne.SourceDonnees> {

	@Override
	protected Personne.SourceDonnees convert(@NotNull PersonDataSource value) {
		switch (value) {
		case BRUTES_RCENT:
			return Personne.SourceDonnees.BRUTES_RCENT;
		case ENRICHIES_RCPERS:
			return Personne.SourceDonnees.ENRICHIES_RCPERS;
		case BRUTES_RCENT_AVEC_RCPERS_INDISPONIBLE:
			return Personne.SourceDonnees.BRUTES_RCENT_AVEC_RCPERS_INDISPONIBLE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value.name(), value.getClass().getSimpleName()));
		}
	}
}

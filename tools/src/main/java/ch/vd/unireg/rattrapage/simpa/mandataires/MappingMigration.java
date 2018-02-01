package ch.vd.unireg.rattrapage.simpa.mandataires;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingMigration {

	public static class Key {

		private final TypeTiers typeTiers;
		private final long idSimpa;

		public Key(TypeTiers typeTiers, long idSimpa) {
			this.typeTiers = typeTiers;
			this.idSimpa = idSimpa;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Key key = (Key) o;
			return idSimpa == key.idSimpa && typeTiers == key.typeTiers;
		}

		@Override
		public int hashCode() {
			int result = typeTiers.hashCode();
			result = 31 * result + (int) (idSimpa ^ (idSimpa >>> 32));
			return result;
		}
	}

	private final Key key;
	private final long idUnireg;

	private static final Pattern PATTERN = Pattern.compile("(ETABLISSEMENT|INDIVIDU);(\\d+);(\\d+)");

	public static MappingMigration of(String string) throws ParseException {
		final Matcher matcher = PATTERN.matcher(string);
		if (!matcher.matches()) {
			throw new ParseException("Ligne non-reconnue : " + string, 0);
		}
		return new MappingMigration(TypeTiers.valueOf(matcher.group(1)),
		                            Long.parseLong(matcher.group(2)),
		                            Long.parseLong(matcher.group(3)));
	}

	private MappingMigration(TypeTiers typeTiers, long idSimpa, long idUnireg) {
		this.key = new Key(typeTiers, idSimpa);
		this.idUnireg = idUnireg;
	}

	public Key getKey() {
		return key;
	}

	public TypeTiers getTypeTiers() {
		return key.typeTiers;
	}

	public long getIdSimpa() {
		return key.idSimpa;
	}

	public long getIdUnireg() {
		return idUnireg;
	}
}

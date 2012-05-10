package ch.vd.uniregctb.type;

public enum TypeAdresseCivil {
	SECONDAIRE,
	PRINCIPALE,
	COURRIER,
	TUTEUR;

	public static TypeAdresseCivil fromDbValue(String val) {
		if (val == null) {
			return null;
		}
		if ("C".equals(val)) {
			return TypeAdresseCivil.COURRIER;
		}
		else if ("P".equals(val)) {
			return TypeAdresseCivil.PRINCIPALE;
		}
		else if ("S".equals(val)) {
			return TypeAdresseCivil.SECONDAIRE;
		}
		else if ("T".equals(val)) {
			return TypeAdresseCivil.TUTEUR;
		}
		else {
			throw new IllegalArgumentException("Code d'adresse civil inconnu = [" + val + "]");
		}
	}
	
	public String toDbValue() {
		switch (this) {
		case COURRIER:
			return "C";
		case PRINCIPALE:
			return "P";
		case SECONDAIRE:
			return "S";
		case TUTEUR:
			return "T";
		default:
			throw new IllegalArgumentException("Type d'adresse civil inconnu = [" + this + "]");
		}
	}
}
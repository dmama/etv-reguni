package ch.vd.uniregctb.type;

public enum TypeAdresseCivil {
	SECONDAIRE("secondaire"),
	PRINCIPALE("principale"),
	COURRIER("courrier"),
	TUTEUR("tutelle"),
	CASE_POSTALE("case postale");

	private String description;

	TypeAdresseCivil(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public static TypeAdresseCivil fromDbValue(String val) {
		if (val == null) {
			return null;
		}
		switch (val) {
		case "C":
			return TypeAdresseCivil.COURRIER;
		case "P":
			return TypeAdresseCivil.PRINCIPALE;
		case "S":
			return TypeAdresseCivil.SECONDAIRE;
		case "T":
			return TypeAdresseCivil.TUTEUR;
		case "B":
			return TypeAdresseCivil.CASE_POSTALE;
		default:
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
		case CASE_POSTALE:
			return "B";
		default:
			throw new IllegalArgumentException("Type d'adresse civil inconnu = [" + this + "]");
		}
	}
}
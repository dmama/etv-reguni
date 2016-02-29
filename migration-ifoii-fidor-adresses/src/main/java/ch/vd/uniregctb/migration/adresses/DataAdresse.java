package ch.vd.uniregctb.migration.adresses;

import ch.vd.registre.base.date.RegDate;

final class DataAdresse {

	protected final long id;
	protected final RegDate dateFin;
	protected final String rue;
	protected final Integer noOrdrePoste;
	protected final Integer noRue;
	protected final long tiersId;
	protected final RegDate dateAnnulation;

	DataAdresse(long id, RegDate dateFin, String rue, Integer noOrdrePoste, Integer noRue, long tiersId, RegDate dateAnnulation) {
		this.id = id;
		this.dateFin = dateFin;
		this.rue = rue;
		this.noOrdrePoste = noOrdrePoste;
		this.noRue = noRue;
		this.tiersId = tiersId;
		this.dateAnnulation = dateAnnulation;
	}

	private static String enquote(String str) {
		if (str == null) {
			return "null";
		}
		return String.format("'%s'", str);
	}

	@Override
	public String toString() {
		return "DataAdresse{" +
				"id=" + id +
				", tiersId=" + tiersId +
				", dateAnnulation=" + dateAnnulation +
				", dateFin=" + dateFin +
				", rue=" + enquote(rue) +
				", noOrdrePoste=" + noOrdrePoste +
				", noRue=" + noRue +
				'}';
	}
}

package ch.vd.uniregctb.common;

import java.sql.Timestamp;
import java.util.Date;

public interface Loggable extends Annulable {

	public String getLogCreationUser();

	public Date getLogCreationDate();

	public String getLogModifUser();

	public Timestamp getLogModifDate();

	public Date getAnnulationDate();

	public String getAnnulationUser();

}

package ch.vd.uniregctb.common;

import java.sql.Timestamp;
import java.util.Date;

public interface Loggable extends Annulable {

	String getLogCreationUser();

	Date getLogCreationDate();

	String getLogModifUser();

	Timestamp getLogModifDate();

	Date getAnnulationDate();

	String getAnnulationUser();

}

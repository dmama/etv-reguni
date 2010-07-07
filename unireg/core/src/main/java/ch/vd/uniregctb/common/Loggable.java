package ch.vd.uniregctb.common;

import java.util.Date;

public interface Loggable {

	public String getLogCreationUser();

	public Date getLogCreationDate();

	public String getLogModifUser();

	public Date getLogModifDate();

	public Date getAnnulationDate();

	public String getAnnulationUser();

	public boolean isAnnule();
}

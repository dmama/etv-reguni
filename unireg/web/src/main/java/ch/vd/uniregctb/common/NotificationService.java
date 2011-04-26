package ch.vd.uniregctb.common;

import java.util.Date;

/**
 * Service de notification d'exception.
 */
public interface NotificationService {
	public void sendNotification(Exception exception, Date date, String applicationName, String url, String user, int occurenceCount);
}

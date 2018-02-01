package ch.vd.unireg.common;

import java.util.Date;

/**
 * Service de notification d'exception.
 */
public interface NotificationService {
	void sendNotification(Exception exception, Date date, String applicationName, String url, String user, int occurenceCount);
}

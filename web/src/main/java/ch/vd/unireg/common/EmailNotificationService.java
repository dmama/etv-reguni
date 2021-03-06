package ch.vd.unireg.common;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import ch.vd.unireg.utils.LogLevel;

/**
 * Service de notification d'exceptions par email.
 *
 * @see http://developingdeveloper.wordpress.com/2008/03/09/handling-exceptions-in-spring-mvc-part-2/
 */
public class EmailNotificationService implements NotificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

	private final MailSender mailSender;
	private final SimpleMailMessage templateMessage;

	public EmailNotificationService(MailSender mailSender, SimpleMailMessage templateMessage) {
		this.mailSender = mailSender;
		this.templateMessage = templateMessage;
	}

	@Override
	public void sendNotification(Exception exception, Date date, String applicationName, String url, String user, int occurenceCount) {

		SimpleMailMessage mail = new SimpleMailMessage(this.templateMessage);
		if (mail.getTo().length == 0 || mail.getTo()[0] == null) {
			LOGGER.warn("Email notification message could not sent because no recipient is defined.", exception);
			return;
		}

		// Sujet
		String server = url.replaceAll("([^/]+//[^/]+/[^/]+/[^/]+).*", "$1"); // http://localhost:8080/unireg/tiers/visu.do?id=10097488 => http://localhost:8080/unireg/tiers
		String sujet;
		if (occurenceCount == 1) {
			sujet = "Nouvelle exception [" + exception.getMessage() + "] sur " + server;
		}
		else {
			sujet = occurenceCount + "ème exception [" + exception.getMessage() + "] sur " + server;
		}

		// Entête
		String text;
		if (occurenceCount == 1) {
			text = "Une nouvelle exception est survenue :\n";
		}
		else {
			text = "L'exception est ci-dessous est survenue pour la " + occurenceCount + "ème fois :\n";
		}
		text += " - date        : " + DATE_FORMAT.format(date) + '\n';
		text += " - application : " + applicationName + '\n';
		text += " - url         : " + url + '\n';
		text += " - utilisateur : " + user;

		// Extrait la call-stack
		text += "\n\n" + ExceptionUtils.getStackTrace(exception);
		mail.setSubject(sujet);
		mail.setText(text);

		try {
			this.mailSender.send(mail);
		}
		catch (MailException ex) {
			// simply log it and go on…
			LogLevel.log(LOGGER, LogLevel.Level.FATAL, "Email notification message could not sent", ex);
		}
	}

}

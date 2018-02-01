package ch.vd.unireg.reqdes;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "REQDES_ERREUR")
public class ErreurTraitement extends HibernateEntity {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErreurTraitement.class);

	public enum TypeErreur {
		ERROR,
		WARNING
	}

	private Long id;
	private String message;
	private String callstack;
	private TypeErreur type;

	public ErreurTraitement() {
	}

	public ErreurTraitement(TypeErreur type, String message) {
		this.message = message;
		this.type = type;
	}

	@Transient
	@Override
	public Object getKey() {
		return getId();
	}

	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "MESSAGE", length = LengthConstants.REQDES_ERREUR_MESSAGE)
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = truncate(message, LengthConstants.REQDES_ERREUR_MESSAGE, true);
	}

	@Column(name = "CALLSTACK", length = LengthConstants.MAXLEN)
	public String getCallstack() {
		return callstack;
	}

	public void setCallstack(String callstack) {
		this.callstack = truncate(callstack, LengthConstants.MAXLEN, false);
	}

	@Column(name = "TYPE", length = LengthConstants.REQDES_ERREUR_TYPE, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public TypeErreur getType() {
		return type;
	}

	public void setType(TypeErreur type) {
		this.type = type;
	}

	private static String truncate(String source, int maxLen, boolean log) {
		final String truncated = StringUtils.abbreviate(source, maxLen);
		if (log && source != null && source.length() > truncated.length()) {
			LOGGER.warn(String.format("Value too big to fit, initial value was '%s'", source));
		}
		return truncated;
	}
}

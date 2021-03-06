package ch.vd.unireg.evenement.civil.ech;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.evenement.common.EvenementErreur;
import ch.vd.unireg.type.TypeEvenementErreur;

@Entity
@Table(name = "EVENEMENT_CIVIL_ECH_ERREUR")
public class EvenementCivilEchErreur extends HibernateEntity implements EvenementErreur {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchErreur.class);

	private Long id;
	private String message;
	private String callstack;
	private TypeEvenementErreur type;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	@Column(name = "MESSAGE", length = LengthConstants.EVTCIVILERREUR_MESSAGE)
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
		validateMessage();
	}

	@Override
	@Column(name = "CALLSTACK", length = LengthConstants.MAXLEN)
	public String getCallstack() {
		return callstack;
	}

	public void setCallstack(String callstack) {
		if (callstack != null && callstack.length() > LengthConstants.MAXLEN) {
			this.callstack = callstack.substring(0, LengthConstants.MAXLEN);
		}
		else {
			this.callstack = callstack;
		}
	}

	@Override
	@Column(name = "TYPE", nullable = false, length = LengthConstants.EVTCIVILERREUR_TYPE)
	@Type(type = "ch.vd.unireg.hibernate.TypeEvenementErreurUserType")
	public TypeEvenementErreur getType() {
		return type;
	}

	public void setType(TypeEvenementErreur type) {
		this.type = type;
	}

	private void validateMessage() {
		if (message != null) {
			if (message.length() > LengthConstants.EVTCIVILERREUR_MESSAGE) {
				LOGGER.warn("Message d'erreur d'événement civil tronqué ; valeur initiale : " + message);
				message = message.substring(0, LengthConstants.EVTCIVILERREUR_MESSAGE);
			}
		}
	}
}

package ch.vd.uniregctb.evenement.civil.ech;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEvenementErreur;

@Entity
@Table(name = "EVENEMENT_CIVIL_ECH_ERREUR")
public class EvenementCivilEchErreur extends HibernateEntity {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchErreur.class);

	private Long id;
	private String message;
	private String callstack;
	private TypeEvenementErreur type;

	public EvenementCivilEchErreur() {
	}

	public EvenementCivilEchErreur(Exception e) {
		this(StringUtils.EMPTY, e);
	}

	public EvenementCivilEchErreur(String m, Exception e) {
		if (e.getMessage() != null) {
			this.message = m + e.getMessage();
		}
		else {
			this.message = m + e.getClass().getSimpleName();
		}
		setCallstack(ExceptionUtils.extractCallStack(e));
		this.type = TypeEvenementErreur.ERROR;
		validateMessage();
	}

	public EvenementCivilEchErreur(String m) {
		this(m, TypeEvenementErreur.ERROR);
	}

	public EvenementCivilEchErreur(String m, TypeEvenementErreur t) {
		this.message = m;
		this.type = t;
		validateMessage();
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "MESSAGE", length = LengthConstants.EVTCIVILERREUR_MESSAGE)
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
		validateMessage();
	}

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

	@Column(name = "TYPE", nullable = false, length = LengthConstants.EVTCIVILERREUR_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEvenementErreurUserType")
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

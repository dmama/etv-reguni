/**
 *
 */
package ch.vd.uniregctb.evenement.civil.externe;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sV8aMGzmEdy3RbcXGfSeBw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_sV8aMGzmEdy3RbcXGfSeBw"
 */
@Entity
@Table(name = "EVENEMENT_CIVIL_ERREUR")
public class EvenementCivilExterneErreur extends HibernateEntity {

	private static final long serialVersionUID = -1077312693852919409L;

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilExterneErreur.class);

	public EvenementCivilExterneErreur() {
	}

	public EvenementCivilExterneErreur(Exception e) {
		this("", e);
	}

	public EvenementCivilExterneErreur(String m, Exception e) {
		if (e.getMessage() != null) {
			message = m + e.getMessage();
		}
		else {
			message = m + e.getClass().getSimpleName();
		}
		setCallstack(ExceptionUtils.extractCallStack(e));
		type = TypeEvenementErreur.ERROR;
		validateMessage();
	}

	public EvenementCivilExterneErreur(String m) {
		this(m, TypeEvenementErreur.ERROR);
	}

	public EvenementCivilExterneErreur(String m, TypeEvenementErreur t) {
		this.message = m;
		this.type = t;
		validateMessage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_trepwGzmEdy3RbcXGfSeBw"
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xTqRYGzmEdy3RbcXGfSeBw"
	 */
	private String message;

	private String callstack;

	private TypeEvenementErreur type;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the id
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_trepwGzmEdy3RbcXGfSeBw?GETTER"
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		// begin-user-code
		return id;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theId the id to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_trepwGzmEdy3RbcXGfSeBw?SETTER"
	 */
	public void setId(Long theId) {
		// begin-user-code
		id = theId;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the message
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xTqRYGzmEdy3RbcXGfSeBw?GETTER"
	 */
	@Column(name = "MESSAGE", length = LengthConstants.EVTCIVILERREUR_MESSAGE)
	public String getMessage() {
		// begin-user-code
		return message;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theMessage the message to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xTqRYGzmEdy3RbcXGfSeBw?SETTER"
	 */
	public void setMessage(String theMessage) {
		// begin-user-code
		message = theMessage;
		validateMessage();
		// end-user-code
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

	@Override
	public String toString() {

		String str = "Msg: "+message;
		str += " Type: "+type;
		return str;
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

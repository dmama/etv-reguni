package ch.vd.uniregctb.common;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.ObjectGetterHelper;

/**
 * @hidden
 *
 * @author jec
 *
 */
@MappedSuperclass
public abstract class HibernateEntity implements Serializable, Loggable {

	private static final long serialVersionUID = 2184891500198493924L;

	public static final Logger DDUMP = Logger.getLogger("debug");

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4odM8EdyUI9FDt56-Qw"
	 */
	private String logCreationUser;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cSMSAOxDEdyck8Nd0o6HOA"
	 */
	private Date logCreationDate;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4o9M8EdyUI9FDt56-Qw"
	 */
	private String logModifUser;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4otM8EdyUI9FDt56-Qw"
	 */
	private Timestamp logModifDate;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the logCreationUser
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4odM8EdyUI9FDt56-Qw?GETTER"
	 */
	@Column(name = "LOG_CUSER", length = LengthConstants.HIBERNATE_LOGUSER)
	public String getLogCreationUser() {
		// begin-user-code
		return logCreationUser;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theLogCreationUser the logCreationUser to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4odM8EdyUI9FDt56-Qw?SETTER"
	 */
	public void setLogCreationUser(String theLogCreationUser) {
		// begin-user-code
		logCreationUser = theLogCreationUser;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the logCreationDate
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cSMSAOxDEdyck8Nd0o6HOA?GETTER"
	 */
	@Column(name = "LOG_CDATE")
	public Date getLogCreationDate() {
		// begin-user-code
		return logCreationDate;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theLogCreationDate the logCreationDate to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cSMSAOxDEdyck8Nd0o6HOA?SETTER"
	 */
	public void setLogCreationDate(Date theLogCreationDate) {
		// begin-user-code
		logCreationDate = theLogCreationDate;
		// end-user-code
	}


	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the logModifUser
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4o9M8EdyUI9FDt56-Qw?GETTER"
	 */
	@Column(name = "LOG_MUSER", length = LengthConstants.HIBERNATE_LOGUSER)
	public String getLogModifUser() {
		// begin-user-code
		return logModifUser;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theLogModifUser the logModifUser to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4o9M8EdyUI9FDt56-Qw?SETTER"
	 */
	public void setLogModifUser(String theLogModifUser) {
		// begin-user-code
		logModifUser = theLogModifUser;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the logModifDate
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4otM8EdyUI9FDt56-Qw?GETTER"
	 */
	@Version
	@Column(name = "LOG_MDATE")
	public Timestamp getLogModifDate() {
		// begin-user-code
		return logModifDate;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theLogModifDate the logModifDate to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tig4otM8EdyUI9FDt56-Qw?SETTER"
	 */
	public void setLogModifDate(Timestamp theLogModifDate) {
		// begin-user-code
		logModifDate = theLogModifDate;
		// end-user-code
	}
	public void setLogModifMillis(long millis) {
		// begin-user-code
		logModifDate = new Timestamp(millis);
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_860LQCsTEd2YRbfoCS2w9g"
	 */
	private Date annulationDate;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the annulationDate
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_860LQCsTEd2YRbfoCS2w9g?GETTER"
	 */
	@Column(name = "ANNULATION_DATE")
	public Date getAnnulationDate() {
		// begin-user-code
		return annulationDate;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param annulationDate the annulationDate to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_860LQCsTEd2YRbfoCS2w9g?SETTER"
	 */
	public void setAnnulationDate(Date theAnnulationDate) {
		// begin-user-code
		annulationDate = theAnnulationDate;
		// end-user-code
	}

	private String annulationUser;

	@Column(name = "ANNULATION_USER", length = LengthConstants.HIBERNATE_LOGUSER)
	public String getAnnulationUser() {
		return annulationUser;
	}

	public void setAnnulationUser(String annulationUser) {
		this.annulationUser = annulationUser;
	}

	// Methods

	/**
	 * @return une valeur servant de clé d'identification <i>unique</i> de l'objet (i.e. l'id de la ligne associée dans la base de données
	 *         dans la plupart des cas); ou <b>null</b> si l'objet n'est pas encore persisté.
	 */
	@Transient
	public abstract Object getKey();

	/**
	 * [UNIREG-1036] Implémentation du hashCode pour coller au comportement décrit dans {@link #equals(Object)}.
	 *
	 * @see {@link #equals(Object)}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		final Object key = getKey();
		if (key == null) {
			// l'objet n'est pas persisté -> chaque instance est considérée comme différente
			return super.hashCode();
		}
		else {
			// cas normal
			return 31 * key.hashCode();
		}
	}

	/**
	 * [UNIREG-1036] Cette méthode est appelée pour déterminer si deux entités sont identiques, c'est-à-dire si elles correspondent à la
	 * même ligne d'une table de la base (pour plus d'informations, voir https://www.hibernate.org/109.html).
	 * <p>
	 * Dans cette optique et de manière générale, deux entités sont considérées égales si elles possèdent la même clé (= l'id de la ligne
	 * dans la plupart des cas). Dans le cas particulier où l'objet n'est pas encore persisté (= l'id est null), on se rabat sur le test de
	 * l'identité (c'est-à-dire qu'une entité n'est égale qu'à elle-même).
	 *
	 * @see documentation https://www.hibernate.org/109.html
	 * @see #hashCode()
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true; // même objet
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final HibernateEntity other = (HibernateEntity) obj;
		final Object key = getKey();
		final Object otherKey = other.getKey();
		if (key == null || otherKey == null) {
			// dès qu'une des clés est nulle (= non-persisté), l'objet est considéré comme non-égal, c'est-à-dire comme devant être persisté
			return false;
		}
		return key.equals(otherKey);
	}

	/**
	 * Annule une entite
	 *
	 * @param user
	 */
	@Transient
	public void setAnnule(boolean annule) {
		if (annule) {
			annulerPourDate(new Date());
		}
		else {
			setAnnulationDate(null);
			setAnnulationUser(null);
		}
	}

	private void annulerPourDate(Date dateAnnulation) {
		if (dateAnnulation == null) {
			throw new RuntimeException("Une annulation doit se faire avec une date donnée.");
		}
		setAnnulationDate(dateAnnulation);
		if (AuthenticationHelper.getAuthentication() != null) {
			setAnnulationUser(AuthenticationHelper.getCurrentPrincipal());
		}
		else {
			setAnnulationUser("INCONNU");
		}
	}

	@Transient
	public boolean isAnnule() {
		return annulationDate != null;
	}

    /**
	 * Renvoie à l'appelant l'objet lui-même (= this). Cette méthode permet de récupérer l'objet réel lorsqu'il est caché par un proxy.
	 */
	@Transient
	public void tellMeAboutYou(RefParam<HibernateEntity> param) {
		param.ref = this;
	}

	public Object getValue(String name) throws Exception {
		return ObjectGetterHelper.getValue(this, name);
	}

	public void setValue(String name, Object value) throws SQLException {

		if (value != null) {
			try {
				String methodName = "set" + name;
				Class<? extends HibernateEntity> c = getClass();

				// Get the method
				Method m = null;
				Class<?> param1 = value.getClass();
				try {
					m = c.getMethod(methodName, param1);
				} catch (NoSuchMethodException e) {
					m = null;
				}

				if (m == null) {
					if (value instanceof java.sql.Date) {
						param1 = java.util.Date.class;
					} else if (value instanceof java.sql.Time) {
						param1 = java.util.Date.class;
					} else if (value instanceof Integer) {
						param1 = Long.class;
						value = new Long((Integer) value);
					}

					m = c.getMethod(methodName, param1);
				}
				// End get method

				m.setAccessible(true);
				m.invoke(this, value);
				m = null;
			} catch (Exception e) {
				throw new SQLException("Exception: " + e);
			}
		}
	}

	protected void ddump(int nbTabs, String msg) {
		DDUMP.info(getTabs(nbTabs)+msg);
	}

	protected String getTabs(int nbTabs) {
		StringBuffer str = new StringBuffer();
		while (nbTabs > 0) {
			str.append('\t');
			nbTabs--;
		}
		return str.toString();
	}
}

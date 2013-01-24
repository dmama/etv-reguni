package ch.vd.uniregctb.common;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.ObjectGetterHelper;

/**
 * Classe de base pour les entités Unireg
 */
@MappedSuperclass
public abstract class HibernateEntity implements Loggable, Annulable {

	public static final Logger DDUMP = Logger.getLogger("debug");
	private String logCreationUser;
	private Date logCreationDate;
	private String logModifUser;
	private Timestamp logModifDate;

	@Override
	@Column(name = "LOG_CUSER", length = LengthConstants.HIBERNATE_LOGUSER)
	public String getLogCreationUser() {
		return logCreationUser;
	}

	public void setLogCreationUser(String theLogCreationUser) {
		logCreationUser = theLogCreationUser;
	}

	@Override
	@Column(name = "LOG_CDATE")
	public Date getLogCreationDate() {
		return logCreationDate;
	}

	public void setLogCreationDate(Date theLogCreationDate) {
		logCreationDate = theLogCreationDate;
	}

	@Override
	@Column(name = "LOG_MUSER", length = LengthConstants.HIBERNATE_LOGUSER)
	public String getLogModifUser() {
		return logModifUser;
	}

	public void setLogModifUser(String theLogModifUser) {
		logModifUser = theLogModifUser;
	}

	@Override
	@Version
	@Column(name = "LOG_MDATE")
	public Timestamp getLogModifDate() {
		return logModifDate;
	}

	public void setLogModifDate(Timestamp theLogModifDate) {
		logModifDate = theLogModifDate;
	}
	public void setLogModifMillis(long millis) {
		logModifDate = new Timestamp(millis);
	}

	private Date annulationDate;
	@Override
	@Column(name = "ANNULATION_DATE")
	public Date getAnnulationDate() {
		return annulationDate;
	}

	public void setAnnulationDate(@Nullable Date theAnnulationDate) {
		annulationDate = theAnnulationDate;
	}

	private String annulationUser;

	@Override
	@Column(name = "ANNULATION_USER", length = LengthConstants.HIBERNATE_LOGUSER)
	public String getAnnulationUser() {
		return annulationUser;
	}

	public void setAnnulationUser(@Nullable String annulationUser) {
		this.annulationUser = annulationUser;
	}

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
	 * @see <a href="https://community.jboss.org/wiki/EqualsAndHashCode">Hibernate Documentation</a>
	 * @see #hashCode()
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings({"SimplifiableIfStatement"})
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
	 * @param annule ...
     *  true annule l'entity, false la "désannule"
	 */
	@Transient
	public void setAnnule(boolean annule) {
		if (annule) {
			annulerPourDate(DateHelper.getCurrentDate());
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

	@Override
	@Transient
	public boolean isAnnule() {
		return annulationDate != null;
	}

    /**
	 * Renvoie à l'appelant l'objet lui-même (= this). Cette méthode permet de récupérer l'objet réel lorsqu'il est caché par un proxy.
     * 
     * @param param parametre pour stocker le resultat
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
				Method m;
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
						value = ((Integer) value).longValue();
					}

					m = c.getMethod(methodName, param1);
				}
				// End get method
				m.setAccessible(true);
				m.invoke(this, value);
			} catch (Exception e) {
				throw new SQLException("Exception: " + e);
			}
		}
	}

	protected void ddump(int nbTabs, String msg) {
		DDUMP.info(getTabs(nbTabs)+msg);
	}

	protected String getTabs(int nbTabs) {
		StringBuilder str = new StringBuilder();
		while (nbTabs > 0) {
			str.append('\t');
			nbTabs--;
		}
		return str.toString();
	}
}

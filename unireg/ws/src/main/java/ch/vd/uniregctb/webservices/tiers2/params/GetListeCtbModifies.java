package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

import ch.vd.uniregctb.webservices.common.UserLogin;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class GetListeCtbModifies {
		/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;


	/** Critère sur la date de début de recherche. */
	@XmlElement(required = true)
	public Date dateDebutRecherche;

	/** Critère sur la date de fin de recherche. */
	@XmlElement(required = true)
	public Date dateFinRecherche;

	@Override
	public String toString() {
		return "GetListeCtbModifies{" +
				"login=" + login +
				", dateDebutRecherche=" + dateDebutRecherche +
				", dateFinRecherche='" + dateFinRecherche +
				'}';
	}
}

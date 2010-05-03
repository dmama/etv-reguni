package ch.vd.uniregctb.webservices.tiers2.params;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.DemandeQuittancementDeclaration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuittancerDeclarations")
public class QuittancerDeclarations {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	/**
	 * Les demandes de quittancement des déclarations d'impôt ordinaires.
	 */
	@XmlElement(required = true)
	public List<DemandeQuittancementDeclaration> demandes = new ArrayList<DemandeQuittancementDeclaration>();

	@Override
	public String toString() {
		return "QuittancerDeclarations{" +
				"login=" + login +
				", demandes=" + (demandes == null ? "null" : Arrays.toString(demandes.toArray())) +
				'}';
	}
}

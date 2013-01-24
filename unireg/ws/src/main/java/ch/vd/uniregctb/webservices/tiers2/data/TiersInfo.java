package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Information disponibles sur un tiers retournées lors du recherche.
 * <p/>
 * Les informations détaillées des tiers (fors fiscaux, adresses, ...) sont disponibles au travers des méthodes getTiers.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>partyInfoType</i> (xml) / <i>PartyInfo</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TiersInfo", propOrder = {
		"numero", "nom1", "nom2", "dateNaissance", "rue", "npa", "localite", "pays", "type"
})
public class TiersInfo {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>number</i>.
	 */
	public long numero;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>name1</i>.
	 */
	public String nom1;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>name2</i>.
	 */
	public String nom2;

	/**
	 * La date de naissance avec le format YYYYMMDD.
	 * <p/>
	 * <b>Attention:</b> cette date peut être partielle au niveau du jour et du mois (ex: "197311" - jour de naissance inconnu, "1973" - jour et mois de naissance inconnus).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateOfBirth</i>.
	 */
	public String dateNaissance;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>street</i>.
	 */
	public String rue;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>swissZipCode</i>.
	 */
	public String npa;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>town</i>.
	 */
	public String localite;

	/**
	 * Contient le nom en toutes lettres (Suisse, Albanie, Japon, ...) du pays de l'adresse.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>country</i>.
	 */
	public String pays;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>type</i>.
	 */
	public Tiers.Type type;

	public TiersInfo() {
	}

	public TiersInfo(ch.vd.uniregctb.indexer.tiers.TiersIndexedData right) {
		numero = right.getNumero();
		nom1 = right.getNom1();
		nom2 = right.getNom2();
		rue = right.getRue();
		npa = right.getNpa();
		localite = right.getLocalite();
		pays = right.getPays();
		dateNaissance = right.getDateNaissance();
		type = DataHelper.getType(right);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (numero ^ (numero >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TiersInfo other = (TiersInfo) obj;
		return numero == other.numero;
	}
}

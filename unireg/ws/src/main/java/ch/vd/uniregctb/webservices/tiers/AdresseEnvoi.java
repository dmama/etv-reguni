package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Représente les 6 lignes d'adresses d'un tiers formattées selon les recommandations de la poste suisse.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdresseEnvoi", propOrder = {
		"ligne1", "ligne2", "ligne3", "ligne4", "ligne5", "ligne6", "isSuisse"
})
public class AdresseEnvoi {

	@XmlElement(required = true)
	public String ligne1;

	@XmlElement(required = true)
	public String ligne2;

	@XmlElement(required = true)
	public String ligne3;

	@XmlElement(required = true)
	public String ligne4;

	@XmlElement(required = false)
	public String ligne5;

	@XmlElement(required = false)
	public String ligne6;

	@XmlElement(required = true)
	public boolean isSuisse;

	public AdresseEnvoi() {
	}

	public AdresseEnvoi(String ligne1, String ligne2, String ligne3, String ligne4, String ligne5, String ligne6, boolean isSuisse) {
		this.ligne1 = ligne1;
		this.ligne2 = ligne2;
		this.ligne3 = ligne3;
		this.ligne4 = ligne4;
		this.ligne5 = ligne5;
		this.ligne6 = ligne6;
		this.isSuisse = isSuisse;
	}

	public AdresseEnvoi(ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee adresse) {
		this.ligne1 = adresse.getLigne1();
		this.ligne2 = adresse.getLigne2();
		this.ligne3 = adresse.getLigne3();
		this.ligne4 = adresse.getLigne4();
		this.ligne5 = adresse.getLigne5();
		this.ligne6 = adresse.getLigne6();
		this.isSuisse = adresse.isSuisse();
	}
}

package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Le for de gestion permet de retrouver l’office d’impôt compétent <i>ratione loci</i> du contribuable. Il ne s’agit pas à proprement parler d’une
 * notion fiscale, mais uniquement d’une notion propre à l’organisation de l’ACI
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ForGestion", propOrder = {
		"dateDebut", "dateFin", "noOfsCommune","nomCommune"
})
public class ForGestion{

	/**
	 * La date de début de validité du for de gestion.
	 * <p>
	 * <b>Note:</b> fondamentalement, tout for de gestion possède un début de validité; mais pour des raisons de performance, cette valeur
	 * n'est renseignée que sur les fors rattachés à la classe TiersHisto.
	 */
	@XmlElement(required = false)
	public Date dateDebut;

	/** La date de fin de validité du for de gestion. Si le for est toujours actif, cette date n'est pas renseignée. */
	@XmlElement(required = false)
	public Date dateFin;

	/** Numéro OFS étendu de la commune vaudoise (qui permet de déduire l'office d'impôt compétent) */
	@XmlElement(required = true)
	public int noOfsCommune;
	/**Nom de la commune correspondant au numéro de l'autorité fiscal*/
	@XmlElement(required = true)
	public String nomCommune;

	public ForGestion(){
	}

	public ForGestion(int noOfsCommune, Context context) {
		this.dateDebut = null;
		this.dateFin = null;
		this.noOfsCommune = context.noOfsTranslator.translateCommune(noOfsCommune);
		this.nomCommune = DataHelper.getNomCommune(this.noOfsCommune, context.infraService);
	}

	public ForGestion(ch.vd.uniregctb.tiers.ForGestion forGestion, Context context) {
		this.dateDebut = DataHelper.coreToWeb(forGestion.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(forGestion.getDateFin());
		this.noOfsCommune = context.noOfsTranslator.translateCommune(forGestion.getNoOfsCommune());
		this.nomCommune = DataHelper.getNomCommune(this.noOfsCommune, context.infraService);
	}
}

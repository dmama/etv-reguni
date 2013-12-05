package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * <b>Dans la version 3 du web-service :</b> <i>taxDeclarationStatusType</i> (xml) / <i>TaxDeclarationStatus</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EtatDeclaration", propOrder = {
		"etat", "dateObtention", "dateAnnulation"
})
public class EtatDeclaration {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxDeclarationStatusTypeType</i> (xml) / <i>TaxDeclarationStatusType</i> (client java)
	 */
	@XmlType(name = "TypeEtatDeclaration")
	@XmlEnum(String.class)
	public static enum Type {
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SENT</i>.
		 */
		EMISE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SUMMONS_SENT</i>.
		 */
		SOMMEE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>EXPIRED</i>.
		 */
		ECHUE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>RETURNED</i>.
		 */
		RETOURNEE;

		public static Type fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>type</i>.
	 */
	@XmlElement(required = true)
	public Type etat;

	/**
	 * Date d'obtention de l'état décrit ici (pour le cas de la sommation, ce n'est pas la date de traitement métier de la sommation qui est indiquée ici, i.e. la vraie date d'obtention, mais la date
	 * indiquée sur le courrier envoyé au contribuable)
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>.
	 */
	@XmlElement(required = true)
	public Date dateObtention;

	/**
	 * Date à laquelle l'état a été annulé, ou <b>null</b> s'il n'est pas annulé.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>cancellationDate</i>.
	 */
	@XmlElement(required = false)
	public Date dateAnnulation;

	public EtatDeclaration() {
	}

	public EtatDeclaration(ch.vd.uniregctb.declaration.EtatDeclaration etatDeclaration) {
		this.etat = EnumHelper.coreToWeb(etatDeclaration.getEtat());
		this.dateObtention = DataHelper.coreToWeb(getDateObtentionFieldContent(etatDeclaration));
		this.dateAnnulation = DataHelper.coreToWeb(etatDeclaration.getAnnulationDate());
	}

	/**
	 * [UNIREG-3407] Pour les états de sommation, c'est la date de l'envoi du courrier qu'il faut renvoyer
	 *
	 * @param etatDeclaration etat de la déclaration
	 * @return valeur de la date à mettre dans le champ {@link #dateObtention}
	 */
	private static RegDate getDateObtentionFieldContent(ch.vd.uniregctb.declaration.EtatDeclaration etatDeclaration) {
		final RegDate date;
		if (etatDeclaration instanceof ch.vd.uniregctb.declaration.EtatDeclarationSommee) {
			final ch.vd.uniregctb.declaration.EtatDeclarationSommee etatSommee = (ch.vd.uniregctb.declaration.EtatDeclarationSommee) etatDeclaration;
			date = etatSommee.getDateEnvoiCourrier();
		}
		else {
			date = etatDeclaration.getDateObtention();
		}
		return date;
	}
}

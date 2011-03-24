package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EtatDeclaration", propOrder = {
		"etat", "dateObtention", "dateAnnulation"
})
public class EtatDeclaration {

	@XmlType(name = "TypeEtatDeclaration")
	@XmlEnum(String.class)
	public static enum Type {
		EMISE,
		SOMMEE,
		ECHUE,
		RETOURNEE;

		public static Type fromValue(String v) {
			return valueOf(v);
		}
	}

	@XmlElement(required = true)
	public Type etat;

	/**
	 * Date d'obtention de l'état décrit ici (pour le cas de la sommation, ce n'est pas la date de traitement
	 * métier de la sommation qui est indiquée ici, i.e. la vraie date d'obtention, mais la date indiquée sur le
	 * courrier envoyé au contribuable)
	 */
	@XmlElement(required = true)
	public Date dateObtention;

	/** Date à laquelle l'état a été annulé, ou <b>null</b> s'il n'est pas annulé. */
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

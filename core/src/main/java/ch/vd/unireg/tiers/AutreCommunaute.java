package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.FormeJuridique;

/**
 * Organisation inconnue du registre des personnes morales de l'ACI.
 * Comprend également certains services publics : office du tuteur général, offices des poursuites, administrations fiscales...
 */
@Entity
@DiscriminatorValue("AutreCommunaute")
public class AutreCommunaute extends ContribuableImpositionPersonnesMorales {

	// Numéros générés pour AutreCommunauté et CollectiviteAdministrative
	public static final int CAAC_GEN_FIRST_ID = 2000000;
	public static final int CAAC_GEN_LAST_ID = 2999999;

	/**
	 * Nom de l'entreprise, de l'organisation ou de l'autorité.
	 * Est appelé dans certains contextes raison sociale ou raison de commerce.
	 * Par exemple, "Soladest SA" ou "Département fédéral des finances"
	 */
	private String nom;
	private FormeJuridique formeJuridique;

	@Column(name = "AC_NOM", length = LengthConstants.TIERS_NOM)
	public String getNom() {
		return nom;
	}

	public void setNom(String theNom) {
		nom = theNom;
	}

	@Column(name = "AC_FORME_JURIDIQUE", length = LengthConstants.AC_FORME)
	@Type(type = "ch.vd.unireg.hibernate.FormeJuridiqueUserType")
	public FormeJuridique getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridique theFormeJuridique) {
		formeJuridique = theFormeJuridique;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Autre tiers";
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.AutreCommunaute;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.AUTRE_COMMUNAUTE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final AutreCommunaute other = (AutreCommunaute) obj;
		return ComparisonHelper.areEqual(formeJuridique, other.formeJuridique)
				&& ComparisonHelper.areEqual(nom, other.nom);
	}
}

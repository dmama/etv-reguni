package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Informations associées à une personne morale pour une date donnée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class PersonneMorale extends Contribuable {

	/** Désignation abrégée de la PM. */
	@XmlElement(required = true)
	public String designationAbregee;

	/** Première ligne de la raison sociale. */
	@XmlElement(required = true)
	public String raisonSociale1;

	/** Deuxième ligne de la raison sociale. */
	@XmlElement(required = false)
	public String raisonSociale2;

	/** Troisième ligne de la raison sociale. */
	@XmlElement(required = false)
	public String raisonSociale3;

	/** Date de fin du dernier exercice commercial. */
	@XmlElement(required = false)
	public Date dateFinDernierExerciceCommercial;

	/** Date de bouclement futur de la PM. */
	@XmlElement(required = false)
	public Date dateBouclementFutur;

	/** Numéro IPMRO (n'existe pas sur toutes les PMs). */
	@XmlElement(required = false)
	public String numeroIPMRO;

	/** Le siège valide à la date demandée. */
	@XmlElement(required = false)
	public Siege siege;

	/** La forme juridique valide à la date demandée. */
	@XmlElement(required = false)
	public FormeJuridique formeJuridique;

	/** Le capital valide à la date demandée. */
	@XmlElement(required = false)
	public Capital capital;

	/** Le régime fiscal ICC (Impôt Canton-Commune) valide à la date demandée. */
	@XmlElement(required = false)
	public RegimeFiscal regimeFiscalICC;

	/** Le régime fiscal IFD (Impôt Fédéral Direct) valide à la date demandée. */
	@XmlElement(required = false)
	public RegimeFiscal regimeFiscalIFD;

	/** L'état de la PM valide à la date demandée. */
	@XmlElement(required = false)
	public EtatPM etat;

	public PersonneMorale() {
	}

	public PersonneMorale(PersonneMorale pm, Set<TiersPart> parts) {
		super(pm, parts);
		this.designationAbregee = pm.designationAbregee;
		this.raisonSociale1 = pm.raisonSociale1;
		this.raisonSociale2 = pm.raisonSociale2;
		this.raisonSociale3 = pm.raisonSociale3;
		copyParts(pm, parts);
	}

	private final void copyParts(PersonneMorale pm, Set<TiersPart> parts) {

		if (parts != null && parts.contains(TiersPart.SIEGES)) {
			this.siege = pm.siege;
		}

		if (parts != null && parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			this.formeJuridique = pm.formeJuridique;
		}

		if (parts != null && parts.contains(TiersPart.CAPITAUX)) {
			this.capital = pm.capital;
		}

		if (parts != null && parts.contains(TiersPart.REGIMES_FISCAUX)) {
			this.regimeFiscalICC = pm.regimeFiscalICC;
			this.regimeFiscalIFD = pm.regimeFiscalIFD;
		}

		if (parts != null && parts.contains(TiersPart.ETATS_PM)) {
			this.etat = pm.etat;
		}
	}

	@Override
	public Tiers clone(Set<TiersPart> parts) {
		return new PersonneMorale(this, parts);
	}

	@Override
	public void copyPartsFrom(Tiers tiers, Set<TiersPart> parts) {
		super.copyPartsFrom(tiers, parts);
		copyParts((PersonneMorale) tiers, parts);
	}
}

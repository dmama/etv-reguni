package ch.vd.uniregctb.evenement.organisation.view;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationCriteria;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationCriteriaView extends EvenementOrganisationCriteria<ch.vd.uniregctb.type.TypeEvenementOrganisation> {

	public static final String TOUS = "TOUS";

	private static final long serialVersionUID = 6031455278880401726L;

	private String numeroOrganisationFormatte;
	private String numeroCTBFormatte;
	private String typeEvenement;
	private String etatEvenement;
	private String formeJuridiqueEvenement;

	private boolean modeLotEvenement;

	protected Class<TypeEvenementOrganisation> getTypeClass() {
		return TypeEvenementOrganisation.class;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getTypeEvenement() {
		return typeEvenement;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setTypeEvenement(String typeEvenement) {
		if (TOUS.equals(typeEvenement)) {
			setType(null);
		}
		else {
			ch.vd.uniregctb.type.TypeEvenementOrganisation type = Enum.valueOf(getTypeClass(), typeEvenement);
			if (type != null) {
				setType(type);
			}
		}
		this.typeEvenement = typeEvenement;
	}

	@Override
	public void setType(@Nullable TypeEvenementOrganisation type) {
		super.setType(type);
		if (type != null) {
			this.typeEvenement = type.name();
		}
		else {
			this.typeEvenement = TOUS;
		}
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getEtatEvenement() {
		return etatEvenement;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEtatEvenement(String etatEvenement) {
		if (TOUS.equals(etatEvenement)) {
			setEtat(null);
		}
		else {
			final EtatEvenementOrganisation etat = EtatEvenementOrganisation.valueOf(etatEvenement);
			if (etat != null) {
				setEtat(etat);
			}
		}
		this.etatEvenement = etatEvenement;
	}

	@Override
	public void setEtat(@Nullable EtatEvenementOrganisation etat) {
		super.setEtat(etat);
		if (etat != null) {
			this.etatEvenement = etat.name();
		}
		else {
			this.etatEvenement = TOUS;
		}
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getFormeJuridiqueEvenement() {
		return formeJuridiqueEvenement;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setFormeJuridiqueEvenement(String formeJuridiqueEvenement) {
		if (TOUS.equals(formeJuridiqueEvenement)) {
			setFormeJuridique(null);
		}
		else {
			final FormeJuridiqueEntreprise formeJuridique = FormeJuridiqueEntreprise.valueOf(formeJuridiqueEvenement);
			setFormeJuridique(formeJuridique);
		}
		this.formeJuridiqueEvenement = formeJuridiqueEvenement;
	}

	@Override
	public void setFormeJuridique(@Nullable FormeJuridiqueEntreprise formeJuridique) {
		super.setFormeJuridique(formeJuridique);
		if (formeJuridique != null) {
			this.formeJuridiqueEvenement = formeJuridique.name();
		}
		else {
			this.formeJuridiqueEvenement = TOUS;
		}
	}

	public String getNumeroOrganisationFormatte() {
		return numeroOrganisationFormatte;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setNumeroOrganisationFormatte(String numeroOrganisationFormatte) {

		if (StringUtils.isNotEmpty(numeroOrganisationFormatte)) {
			try {
				Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(numeroOrganisationFormatte));
				setNumeroOrganisation(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroOrganisationFormatte))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumeroOrganisation(null);
		}
		this.numeroOrganisationFormatte = numeroOrganisationFormatte;
	}

	public String getNumeroCTBFormatte() {
		return numeroCTBFormatte;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setNumeroCTBFormatte(String numeroCTBFormatte) {
		if (StringUtils.isNotEmpty(numeroCTBFormatte)) {
			try {
				Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(numeroCTBFormatte));
				setNumeroCTB(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroCTBFormatte))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumeroCTB(null);
		}
		this.numeroCTBFormatte = numeroCTBFormatte;
	}

	public boolean isModeLotEvenement() {
		return modeLotEvenement;
	}

	public void setModeLotEvenement(boolean modeLotEvenement) {
		this.modeLotEvenement = modeLotEvenement;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Date getDateEvenementDebut() {
		return RegDate.asJavaDate(getRegDateEvenementDebut());
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDateEvenementDebut(Date dateEvenementDebut) {
		setRegDateEvenementDebut(RegDateHelper.get(dateEvenementDebut));
	}

	@SuppressWarnings("UnusedDeclaration")
	public Date getDateEvenementFin() {
		return RegDate.asJavaDate(getRegDateEvenementFin());
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDateEvenementFin(Date dateEvenementFin) {
		setRegDateEvenementFin(RegDateHelper.get(dateEvenementFin));
	}

}

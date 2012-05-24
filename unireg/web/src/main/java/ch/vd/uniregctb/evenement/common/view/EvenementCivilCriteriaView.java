package ch.vd.uniregctb.evenement.common.view;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.type.EtatEvenementCivil;

abstract public class EvenementCivilCriteriaView<TYPE_EVT extends Enum<TYPE_EVT> > extends EvenementCivilCriteria<TYPE_EVT> {

	public static final String TOUS = "TOUS";

	private static final long serialVersionUID = -440782547474527432L;

	private String numeroIndividuFormatte;
	private String numeroCTBFormatte;
	private String typeEvenement;
	private String etatEvenement;

	@SuppressWarnings("UnusedDeclaration")
	public String getTypeEvenement() {
		return typeEvenement;
	}

	protected abstract Class<TYPE_EVT> getTypeClass();

	@SuppressWarnings("UnusedDeclaration")
	public void setTypeEvenement(String typeEvenement) {
		if (TOUS.equals(typeEvenement)) {
			setType(null);
		}
		else {
			TYPE_EVT type = Enum.valueOf(getTypeClass(), typeEvenement);
			if (type != null) {
				setType(type);
			}
		}
		this.typeEvenement = typeEvenement;
	}

	@Override
	public void setType(@Nullable TYPE_EVT type) {
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
			final EtatEvenementCivil etat = EtatEvenementCivil.valueOf(etatEvenement);
			if (etat != null) {
				setEtat(etat);
			}
		}
		this.etatEvenement = etatEvenement;
	}

	@Override
	public void setEtat(@Nullable EtatEvenementCivil etat) {
		super.setEtat(etat);
		if (etat != null) {
			this.etatEvenement = etat.name();
		}
		else {
			this.etatEvenement = TOUS;
		}
	}

	public String getNumeroIndividuFormatte() {
		return numeroIndividuFormatte;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setNumeroIndividuFormatte(String numeroIndividuFormatte) {

		if (StringUtils.isNotEmpty(numeroIndividuFormatte)) {
			try {
				Long.parseLong(FormatNumeroHelper.removeSpaceAndDash(numeroIndividuFormatte));
				setNumeroIndividu(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroIndividuFormatte))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumeroIndividu(null);
		}
		this.numeroIndividuFormatte = numeroIndividuFormatte;
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

	@SuppressWarnings("UnusedDeclaration")
	public Date getDateEvenementDebut() {
		return RegDate.asJavaDate(getRegDateEvenementDebut());
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDateEvenementDebut(Date dateEvenementDebut) {
		setRegDateEvenementDebut(RegDate.get(dateEvenementDebut));
	}

	@SuppressWarnings("UnusedDeclaration")
	public Date getDateEvenementFin() {
		return RegDate.asJavaDate(getRegDateEvenementFin());
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDateEvenementFin(Date dateEvenementFin) {
		setRegDateEvenementFin(RegDate.get(dateEvenementFin));
	}
}

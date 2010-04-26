package ch.vd.uniregctb.adresse;

import java.util.Date;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;

/*
 * Cette classe permet d'adapter une adresse générique en surchargeant ses dates de début/fin de validité ou sa source.
 */
public class AdresseGeneriqueAdapter implements AdresseGenerique {

	private static final long serialVersionUID = 4279548139998338237L;

	private RegDate debutValiditeSurcharge;
	private RegDate finValiditeSurcharge;
	private AdresseGenerique target;
	private Source source;
	private Boolean isDefault;

	/**
	 * @param adresse
	 *            l'adresse générique à adapter
	 * @param source
	 *            la source de l'adresse à publier
	 * @param isDefault
	 *            vrai si l'adresse représente une adresse par défaut
	 */
	public AdresseGeneriqueAdapter(AdresseGenerique adresse, Source source, Boolean isDefault) {
		Assert.notNull(adresse);
		this.target = adresse;
		this.debutValiditeSurcharge = null;
		this.finValiditeSurcharge = null;
		this.source = source;
		this.isDefault = isDefault;
		optimize();
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
		}
	}

	/**
	 * @param adresse
	 *            l'adresse générique à adapter
	 * @param debut
	 *            (option) une nouvelle adresse de début
	 * @param fin
	 *            (option) une nouvelle adresse de fin
	 * @param isDefault
	 *            vrai si l'adresse représente une adresse par défaut
	 */
	public AdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Boolean isDefault) {
		Assert.notNull(adresse);
		this.target = adresse;
		this.debutValiditeSurcharge = debut;
		this.finValiditeSurcharge = fin;
		this.source = null;
		this.isDefault = isDefault;
		optimize();
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
		}
	}

	/**
	 * @param adresse
	 *            l'adresse générique à adapter
	 * @param debut
	 *            (option) une nouvelle adresse de début
	 * @param fin
	 *            (option) une nouvelle adresse de fin
	 * @param source
	 *            la source de l'adresse à publier
	 * @param isDefault
	 *            vrai si l'adresse représente une adresse par défaut
	 */
	public AdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Source source, Boolean isDefault) {
		Assert.notNull(adresse);
		this.target = adresse;
		this.debutValiditeSurcharge = debut;
		this.finValiditeSurcharge = fin;
		this.source = source;
		this.isDefault = isDefault;
		optimize();
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
		}
	}

	public String getCasePostale() {
		return target.getCasePostale();
	}

	public RegDate getDateDebut() {
		if (debutValiditeSurcharge == null) {
			return target.getDateDebut();
		}
		else {
			return debutValiditeSurcharge;
		}
	}

	public RegDate getDateFin() {
		if (finValiditeSurcharge == null) {
			return target.getDateFin();
		}
		else {
			return finValiditeSurcharge;
		}
	}

	public String getLocalite() {
		return target.getLocalite();
	}

	public String getLocaliteComplete() {
		return target.getLocaliteComplete();
	}

	public String getNumero() {
		return target.getNumero();
	}

	public String getNumeroAppartement() {
		return target.getNumeroAppartement();
	}

	public Integer getNumeroRue() {
		return target.getNumeroRue();
	}

	public int getNumeroOrdrePostal() {
		return target.getNumeroOrdrePostal();
	}

	public String getNumeroPostal() {
		return target.getNumeroPostal();
	}

	public String getNumeroPostalComplementaire() {
		return target.getNumeroPostalComplementaire();
	}

	public Integer getNoOfsPays() {
		return target.getNoOfsPays();
	}

	public String getRue() {
		return target.getRue();
	}

	public String getComplement() {
		return target.getComplement();
	}

	public Source getSource() {
		if (source == null) {
			return target.getSource();
		}
		else {
			return source;
		}
	}

	public boolean isDefault() {
		if (isDefault == null) {
			return target.isDefault();
		}
		else {
			return isDefault;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	/**
	 * Dans le cas où plusieurs AdresseGeneriqueAdapter sont emboîtées, cette méthode recalcule les valeurs surchargées pour ne garder
	 * qu'une instance d'AdresseGeneriqueAdapter.
	 */
	private void optimize() {
		if (target instanceof AdresseGeneriqueAdapter) {
			final AdresseGeneriqueAdapter emboite = (AdresseGeneriqueAdapter) target;

			if (debutValiditeSurcharge == null) {
				debutValiditeSurcharge = emboite.debutValiditeSurcharge;
			}
			if (finValiditeSurcharge == null) {
				finValiditeSurcharge = emboite.finValiditeSurcharge;
			}
			if (source == null) {
				source = emboite.source;
			}
			if (isDefault == null) {
				isDefault = emboite.isDefault;
			}
			target = emboite.target;
		}
	}

	/**
	 * Uniquement pour les unit-tests !
	 */
	protected AdresseGenerique getTarget() {
		return target;
	}

	public Date getAnnulationDate() {
		return target.getAnnulationDate();
	}

	public String getAnnulationUser() {
		return target.getAnnulationUser();
	}

	public Date getLogCreationDate() {
		return target.getLogCreationDate();
	}

	public String getLogCreationUser() {
		return target.getLogCreationUser();
	}

	public Date getLogModifDate() {
		return target.getLogModifDate();
	}

	public String getLogModifUser() {
		return target.getLogModifUser();
	}

	public boolean isAnnule() {
		return target.isAnnule();
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}

	public Long getId() {
		return null;
	}

	public CommuneSimple getCommuneAdresse() {
		return target.getCommuneAdresse();
	}
}

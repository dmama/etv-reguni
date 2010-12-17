package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
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

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private AdresseGenerique target;
	private Source source;
	private boolean isDefault;
	private boolean isAnnule;


	/**
	 * @param adresse   l'adresse générique à adapter
	 * @param source    la source de l'adresse à publier
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 */
	public AdresseGeneriqueAdapter(AdresseGenerique adresse, Source source, Boolean isDefault) {
		Assert.notNull(adresse);
		this.target = adresse;
		this.dateDebut = adresse.getDateDebut();
		this.dateFin = adresse.getDateFin();
		if (source == null) {
			this.source = adresse.getSource();
		}
		else {
			this.source = source;
		}
		if (isDefault == null) {
			this.isDefault = adresse.isDefault();
		}
		else {
			this.isDefault = isDefault;
		}
		this.isAnnule = adresse.isAnnule();
		optimize();
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(dateDebut, dateFin);
		}
	}

	/**
	 * @param adresse   l'adresse générique à adapter
	 * @param debut     (option) une nouvelle adresse de début
	 * @param fin       (option) une nouvelle adresse de fin
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 */
	public AdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Boolean isDefault) {
		Assert.notNull(adresse);
		this.target = adresse;
		this.dateDebut = (debut == null ? adresse.getDateDebut() : debut);
		this.dateFin = (fin == null ? adresse.getDateFin() : fin);
		this.source = adresse.getSource();
		if (isDefault == null) {
			this.isDefault = adresse.isDefault();
		}
		else {
			this.isDefault = isDefault;
		}
		this.isAnnule = adresse.isAnnule();
		optimize();
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(dateDebut, dateFin);
		}
	}

	/**
	 * @param adresse   l'adresse générique à adapter
	 * @param debut     (option) une nouvelle adresse de début
	 * @param fin       (option) une nouvelle adresse de fin
	 * @param source    la source de l'adresse à publier
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 */
	public AdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Source source, Boolean isDefault) {
		Assert.notNull(adresse);
		this.target = adresse;
		this.dateDebut = (debut == null ? adresse.getDateDebut() : debut);
		this.dateFin = (fin == null ? adresse.getDateFin() : fin);
		if (source == null) {
			this.source = adresse.getSource();
		}
		else {
			this.source = source;
		}
		if (isDefault == null) {
			this.isDefault = adresse.isDefault();
		}
		else {
			this.isDefault = isDefault;
		}
		this.isAnnule = adresse.isAnnule();
		optimize();
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(dateDebut, dateFin);
		}
	}

	/**
	 * @param adresse   l'adresse générique à adapter
	 * @param debut     (option) une nouvelle adresse de début
	 * @param fin       (option) une nouvelle adresse de fin
	 * @param source    la source de l'adresse à publier
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 * @param isAnnule  vrai si l'adresse représente une adresse annulée
	 */
	public AdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Source source, Boolean isDefault, Boolean isAnnule) {
		Assert.notNull(adresse);
		this.target = adresse;
		this.dateDebut = (debut == null ? adresse.getDateDebut() : debut);
		this.dateFin = (fin == null ? adresse.getDateFin() : fin);
		if (source == null) {
			this.source = adresse.getSource();
		}
		else {
			this.source = source;
		}
		if (isDefault == null) {
			this.isDefault = adresse.isDefault();
		}
		else {
			this.isDefault = isDefault;
		}
		if (isAnnule == null) {
			this.isAnnule = adresse.isAnnule();
		}
		else {
			this.isAnnule = isAnnule;
		}
		optimize();
		if (!adresse.isAnnule()) {
			DateRangeHelper.assertValidRange(dateDebut, dateFin);
		}
	}

	public String getCasePostale() {
		return target.getCasePostale();
	}

	public final RegDate getDateDebut() {
		return dateDebut;
	}

	public final RegDate getDateFin() {
		return dateFin;
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
		return source;
	}

	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		// [UNIREG-2895] les adresses annulées ne doivent pas être considérées comme valides
		return !isAnnule() && RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	/**
	 * Dans le cas où plusieurs AdresseGeneriqueAdapter sont emboîtées, cette méthode recalcule les valeurs surchargées pour ne garder qu'une instance d'AdresseGeneriqueAdapter.
	 */
	private void optimize() {
		if (target instanceof AdresseGeneriqueAdapter) {
			final AdresseGeneriqueAdapter emboite = (AdresseGeneriqueAdapter) target;
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

	public Timestamp getLogModifDate() {
		return target.getLogModifDate();
	}

	public String getLogModifUser() {
		return target.getLogModifUser();
	}

	public boolean isAnnule() {
		return isAnnule;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}

	public Long getId() {
		if (source == Source.FISCALE) {
			// [UNIREG-2927] dans le cas où l'adresse est de source fiscale, on expose l'id de manière à permettre l'édition de l'adresse dans la GUI
			return target.getId();
		}
		else {
			return null;
		}
	}

	public CommuneSimple getCommuneAdresse() {
		return target.getCommuneAdresse();
	}
}

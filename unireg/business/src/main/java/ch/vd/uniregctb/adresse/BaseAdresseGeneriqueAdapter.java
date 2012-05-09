package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CasePostale;

/*
 * Classe de base commune à toutes les classes qui adaptent - d'une manière ou d'une autre - une adresse générique.
 */
public abstract class BaseAdresseGeneriqueAdapter implements AdresseGenerique {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private AdresseGenerique target;
	private final Source source;
	private final boolean isDefault;
	private final boolean isAnnule;

	/**
	 * @param adresse   l'adresse générique à adapter
	 * @param source    la source de l'adresse à publier
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 */
	public BaseAdresseGeneriqueAdapter(AdresseGenerique adresse, Source source, Boolean isDefault) {
		if (adresse == null) {
			throw new IllegalArgumentException("L'adresse doit être renseignée !");
		}
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
		if (!this.isAnnule) {
			DateRangeHelper.assertValidRange(dateDebut, dateFin);
		}
	}

	/**
	 * @param adresse   l'adresse générique à adapter
	 * @param debut     (option) une nouvelle adresse de début
	 * @param fin       (option) une nouvelle adresse de fin
	 * @param isDefault vrai si l'adresse représente une adresse par défaut
	 */
	public BaseAdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Boolean isDefault) {
		if (adresse == null) {
			throw new IllegalArgumentException("L'adresse doit être renseignée !");
		}
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
		if (!this.isAnnule) {
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
	public BaseAdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Source source, Boolean isDefault) {
		if (adresse == null) {
			throw new IllegalArgumentException("L'adresse doit être renseignée !");
		}
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
		if (!this.isAnnule) {
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
	public BaseAdresseGeneriqueAdapter(AdresseGenerique adresse, RegDate debut, RegDate fin, Source source, Boolean isDefault, Boolean isAnnule) {
		if (adresse == null) {
			throw new IllegalArgumentException("L'adresse doit être renseignée !");
		}
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
		if (!this.isAnnule) {
			DateRangeHelper.assertValidRange(dateDebut, dateFin);
		}
	}

	@Override
	public CasePostale getCasePostale() {
		return target.getCasePostale();
	}

	@Override
	public final RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public final RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String getLocalite() {
		return target.getLocalite();
	}

	@Override
	public String getLocaliteComplete() {
		return target.getLocaliteComplete();
	}

	@Override
	public String getNumero() {
		return target.getNumero();
	}

	@Override
	public String getNumeroAppartement() {
		return target.getNumeroAppartement();
	}

	@Override
	public Integer getNumeroRue() {
		return target.getNumeroRue();
	}

	@Override
	public int getNumeroOrdrePostal() {
		return target.getNumeroOrdrePostal();
	}

	@Override
	public String getNumeroPostal() {
		return target.getNumeroPostal();
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return target.getNumeroPostalComplementaire();
	}

	@Override
	public Integer getNoOfsPays() {
		return target.getNoOfsPays();
	}

	@Override
	public String getRue() {
		return target.getRue();
	}

	@Override
	public String getComplement() {
		return target.getComplement();
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		// [UNIREG-2895] les adresses annulées ne doivent pas être considérées comme valides
		return !isAnnule() && RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	/**
	 * Dans le cas où plusieurs AdresseGeneriqueAdapter sont emboîtées, cette méthode retourne l'adresse générique sous-jacente.
	 */
	protected void optimize() {
		if (this.target instanceof AdresseGeneriqueAdapter) { // [UNIREG-3206] il ne faut pas optimiser les AdresseAutreTiersAdapter !
			final BaseAdresseGeneriqueAdapter emboite = (BaseAdresseGeneriqueAdapter) this.target;
			this.target = emboite.target;
		}
	}

	/**
	 * Uniquement pour les unit-tests !
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AdresseGenerique getTarget() {
		return target;
	}

	@Override
	public Date getAnnulationDate() {
		return target.getAnnulationDate();
	}

	@Override
	public String getAnnulationUser() {
		return target.getAnnulationUser();
	}

	@Override
	public Date getLogCreationDate() {
		return target.getLogCreationDate();
	}

	@Override
	public String getLogCreationUser() {
		return target.getLogCreationUser();
	}

	@Override
	public Timestamp getLogModifDate() {
		return target.getLogModifDate();
	}

	@Override
	public String getLogModifUser() {
		return target.getLogModifUser();
	}

	@Override
	public boolean isAnnule() {
		return isAnnule;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}

	@Override
	public Long getId() {
		if (source.getType() == SourceType.FISCALE) {
			// [UNIREG-2927] dans le cas où l'adresse est de source fiscale, on expose l'id de manière à permettre l'édition de l'adresse dans la GUI
			return target.getId();
		}
		else {
			return null;
		}
	}

	@Override
	public boolean isPermanente() {
		return source.getType() == SourceType.FISCALE && target.isPermanente();
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return target.getNoOfsCommuneAdresse();
	}

	@Override
	public Integer getEgid() {
		return target.getEgid();
	}

	@Override
	public Integer getEwid() {
		return target.getEwid();
	}
}

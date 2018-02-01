package ch.vd.unireg.regimefiscal;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscale;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.CategorieEntreprise;

/**
 * Classe d'encapsulation du régime fiscal consolidé avec son type de régime.
 *
 * @author Raphaël Marmier, 2017-04-10, <raphael.marmier@vd.ch>
 */
public class RegimeFiscalConsolide implements DateRange, Annulable {

	private Long id;

	private RegDate dateDebut;
	private RegDate dateFin;
	private boolean annule;
	private RegimeFiscal.Portee portee;
	private TypeRegimeFiscal typeRegimeFiscal;

	RegimeFiscalConsolide(RegimeFiscal regimeFiscal, TypeRegimeFiscal typeRegimeFiscal) {
		/*
			Contrôle de cohérence: le type de régime fiscal doit correspondre à celui du régime fiscal.
		 */
		if (!regimeFiscal.getCode().equals(typeRegimeFiscal.getCode())) {
			throw new IllegalArgumentException(String.format("Le code du régime fiscal %s ne correspond pas à celui du type: %s (%s)!",
			                                                 regimeFiscal.getCode(), typeRegimeFiscal.getCode(), typeRegimeFiscal.getLibelleAvecCode()));
		}

		this.id = regimeFiscal.getId();

		this.dateDebut = regimeFiscal.getDateDebut();
		this.dateFin = regimeFiscal.getDateFin();
		this.annule = regimeFiscal.getAnnulationDate() != null;
		this.portee = regimeFiscal.getPortee();

		this.typeRegimeFiscal = typeRegimeFiscal;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public Long getId() {
		return id;
	}

	public RegimeFiscal.Portee getPortee() {
		return portee;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}


	public String getCode() {
		return typeRegimeFiscal.getCode();
	}

	public String getLibelle() {
		return typeRegimeFiscal.getLibelle();
	}

	public String getLibelleAvecCode() {
		return typeRegimeFiscal.getLibelleAvecCode();
	}

	public CategorieEntreprise getCategorie() {
		return typeRegimeFiscal.getCategorie();
	}

	/**
	 * @return <code>true</code> si le régime fiscal correspond à celui "en attente de détermination"
	 */
	public boolean isIndetermine() {
		return typeRegimeFiscal.isIndetermine();
	}

	/**
	 * @return <code>true</code> si le régime fiscal correspond à la catégorie "Société de personnes".
	 */
	public boolean isSocieteDePersonnes() {
		return typeRegimeFiscal.isSocieteDePersonnes();
	}

	@Nullable
	public PlageExonerationFiscale getExonerationIBC(int periode) {
		return typeRegimeFiscal.getExonerationIBC(periode);
	}

	@Nullable
	public PlageExonerationFiscale getExonerationICI(int periode) {
		return typeRegimeFiscal.getExonerationICI(periode);
	}

	@Nullable
	public PlageExonerationFiscale getExonerationIFONC(int periode) {
		return typeRegimeFiscal.getExonerationIFONC(periode);
	}

	/**
	 * @param genreImpot genre d'impôt qui nous intéresse
	 * @return une liste des plages d'exonération fiscales du régime fiscal
	 */
	@NotNull
	public List<PlageExonerationFiscale> getExonerations(GenreImpotExoneration genreImpot) {
		return typeRegimeFiscal.getExonerations(genreImpot);
	}
}

package ch.vd.uniregctb.regimefiscal;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscales;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.CategorieEntreprise;

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

	private TypeRegimeFiscal getTypeRegimeFiscal() {
		return typeRegimeFiscal;
	}

	public String getCode() {
		return typeRegimeFiscal.getCode();
	}

	public String getLibelle() {
		return typeRegimeFiscal.getLibelleAvecCode();
	}

	public String getLibelleAvecCode() {
		return getTypeRegimeFiscal().getLibelleAvecCode();
	}

	public CategorieEntreprise getCategorie() {
		return typeRegimeFiscal.getCategorie();
	}

	public boolean isExoneration(int periodeFiscale) {
		return typeRegimeFiscal.isExoneration(periodeFiscale);
	}

	public boolean isExonerationIBC(int periodeFiscale) {
		return getTypeRegimeFiscal().isExonerationIBC(periodeFiscale);
	}

	public boolean isExonerationICI(int periodeFiscale) {
		return getTypeRegimeFiscal().isExonerationICI(periodeFiscale);
	}

	public boolean isExonerationIFONC(int periodeFiscale) {
		return getTypeRegimeFiscal().isExonerationIFONC(periodeFiscale);
	}

	public List<PlageExonerationFiscales> getExonerationsIBC() {
		return getTypeRegimeFiscal().getExonerationsIBC();
	}

	public List<PlageExonerationFiscales> getExonerationsICI() {
		return getTypeRegimeFiscal().getExonerationsICI();
	}

	public List<PlageExonerationFiscales> getExonerationsIFONC() {
		return getTypeRegimeFiscal().getExonerationsIFONC();
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

}

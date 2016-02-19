package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * En l'absence de documentation RCEnt, on ne sait pas trop ce que représente cette classe,
 * à part qu'elle contient parfois des données FOSC.
 *
 * @author Raphaël Marmier, 2016-02-19, <raphael.marmier@vd.ch>
 */
public class PublicationBusiness implements Serializable, DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;

	private final RegDate dateEvenement;
	private final String typeDePublication;
	private final String foscNumeroDeDocument;
	private final RegDate foscDateDePublication;
	private final String foscTexteDeLaPublication;
	private final TypeDeFusion typeDeFusion;
	private final TypeDeReductionDuCapital typeDeReductionDuCapital;
	private final TypeDeTransfere typeDeTransfere;
	private final TypeDeLiquidation typeDeLiquidation;

	public PublicationBusiness(RegDate dateDebut, RegDate dateFin, RegDate dateEvenement, String typeDePublication, String foscNumeroDeDocument, RegDate foscDateDePublication,
	                           String foscTexteDeLaPublication, TypeDeFusion typeDeFusion, TypeDeReductionDuCapital typeDeReductionDuCapital, TypeDeTransfere typeDeTransfere,
	                           TypeDeLiquidation typeDeLiquidation) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.dateEvenement = dateEvenement;
		this.typeDePublication = typeDePublication;
		this.foscNumeroDeDocument = foscNumeroDeDocument;
		this.foscDateDePublication = foscDateDePublication;
		this.foscTexteDeLaPublication = foscTexteDeLaPublication;
		this.typeDeFusion = typeDeFusion;
		this.typeDeReductionDuCapital = typeDeReductionDuCapital;
		this.typeDeTransfere = typeDeTransfere;
		this.typeDeLiquidation = typeDeLiquidation;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public String getTypeDePublication() {
		return typeDePublication;
	}

	public String getFoscNumeroDeDocument() {
		return foscNumeroDeDocument;
	}

	public RegDate getFoscDateDePublication() {
		return foscDateDePublication;
	}

	public String getFoscTexteDeLaPublication() {
		return foscTexteDeLaPublication;
	}

	public TypeDeFusion getTypeDeFusion() {
		return typeDeFusion;
	}

	public TypeDeReductionDuCapital getTypeDeReductionDuCapital() {
		return typeDeReductionDuCapital;
	}

	public TypeDeTransfere getTypeDeTransfere() {
		return typeDeTransfere;
	}

	public TypeDeLiquidation getTypeDeLiquidation() {
		return typeDeLiquidation;
	}
}

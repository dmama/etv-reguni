package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

/**
 * En l'absence de documentation RCEnt, on ne sait pas trop ce que représente cette classe,
 * à part qu'elle contient parfois des données FOSC.
 *
 * @author Raphaël Marmier, 2016-02-19, <raphael.marmier@vd.ch>
 */
public class PublicationBusiness implements Serializable {

	private static final long serialVersionUID = 8011593550198411983L;

	private final RegDate dateEvenement;
	private final TypeDePublicationBusiness typeDePublication;
	private final String foscNumeroDeDocument;
	private final RegDate foscDateDePublication;
	private final String foscTexteDeLaPublication;
	private final TypeDeFusion typeDeFusion;
	private final TypeDeReductionDuCapital typeDeReductionDuCapital;
	private final TypeDeTransfere typeDeTransfere;
	private final TypeDeLiquidation typeDeLiquidation;

	public PublicationBusiness(RegDate dateEvenement, TypeDePublicationBusiness typeDePublication, String foscNumeroDeDocument, RegDate foscDateDePublication,
	                           String foscTexteDeLaPublication, TypeDeFusion typeDeFusion, TypeDeReductionDuCapital typeDeReductionDuCapital, TypeDeTransfere typeDeTransfere,
	                           TypeDeLiquidation typeDeLiquidation) {
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

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public TypeDePublicationBusiness getTypeDePublication() {
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

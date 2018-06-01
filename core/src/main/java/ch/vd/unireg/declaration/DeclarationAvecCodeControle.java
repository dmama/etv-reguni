package ch.vd.unireg.declaration;

import javax.persistence.Column;
import javax.persistence.Entity;

import ch.vd.unireg.common.CodeControleHelper;
import ch.vd.unireg.common.LengthConstants;

/**
 * Classe de déclaration qui possède un code de contrôle pour le dépot en ligne
 */
@Entity
public abstract class DeclarationAvecCodeControle extends DeclarationAvecNumeroSequence {
	/**
	 * Code pour le contrôle du retour électronique de la DI. L'unicité de ce code de contrôle au travers des différentes DI d'un contribuable dépend du type de contribuable : <ul> <li>il est
	 * différent pour toutes les DI d'un contribuable PM, toutes PF confondues&nbsp;;</li> <li>il est le même sur toutes les déclarations d'une période fiscale et d'un contribuable PP donné.</li>
	 * </ul>
	 */
	private String codeControle;
	/**
	 * [SIFISC-2100] Code de segmentation, ou Code Segment, fourni par TAO et utilisé lors de l'émission de la DI suivante
	 * Suffixe du code de routage (le X dans 21-X), encore appelé code segment
	 */
	private Integer codeSegment;


	@Column(name = "CODE_CONTROLE", length = LengthConstants.DI_CODE_CONTROLE)
	public String getCodeControle() {
		return codeControle;
	}

	public void setCodeControle(String codeControle) {
		this.codeControle = codeControle;
	}

	/**
	 * @return un nouveau code de contrôle d'une lettre et de cinq chiffres aléatoires
	 */
	public static String generateCodeControle() {
		return CodeControleHelper.generateCodeControleUneLettreCinqChiffres();
	}

	@Column(name = "CODE_SEGMENT")
	public Integer getCodeSegment() {
		return codeSegment;
	}

	public void setCodeSegment(Integer codeSegment) {
		this.codeSegment = codeSegment;
	}
}

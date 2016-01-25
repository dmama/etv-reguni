package ch.vd.uniregctb.common;

/**
 * Renderer qui affiche un numéro de contribuable
 */
public class NumeroCtbStringRenderer implements StringRenderer<Long> {

	@Override
	public String toString(Long numero) {
		return FormatNumeroHelper.numeroCTBToDisplay(numero);
	}
}

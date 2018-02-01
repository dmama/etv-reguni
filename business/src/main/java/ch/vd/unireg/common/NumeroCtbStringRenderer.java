package ch.vd.uniregctb.common;

/**
 * Renderer qui affiche un numéro de contribuable
 */
public class NumeroCtbStringRenderer implements StringRenderer<Long> {

	/**
	 * Instance singleton pour ce renderer, évite de devoir constamment en créer un nouveau...
	 */
	public static final StringRenderer<Long> INSTANCE = new NumeroCtbStringRenderer();

	@Override
	public String toString(Long numero) {
		return FormatNumeroHelper.numeroCTBToDisplay(numero);
	}
}

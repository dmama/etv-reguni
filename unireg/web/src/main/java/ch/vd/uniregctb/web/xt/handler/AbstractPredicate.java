package ch.vd.uniregctb.web.xt.handler;

import org.apache.commons.collections.Predicate;

public abstract class AbstractPredicate implements Predicate {

	/**
	 * teste saisi par l'utilisateur
	 */
	protected String filter = "";

	public void setFilter(String filter) {
		this.filter = toLowerCaseWithoutAccent(filter);
	}
	
	abstract public boolean evaluate(Object object);
	
	 /** Index du 1er caractere accentué **/
    private static final int MIN = 192;
    /** Index du dernier caractere accentué **/
    private static final int MAX = 255;
    /** tableau de correspondance entre accent / sans accent **/
    private static final char[] map = initMap();
    
	protected String toLowerCaseWithoutAccent(String str) {
		if (str == null)
			return "";
		
		char[] result = str.toCharArray();
    	for(int bcl = 0 ; bcl < result.length ; bcl++) {
    		char c = result[bcl];
    		if(  c >= MIN && c <= MAX && c != 198 && c != 230 && c != 216 && c != 222 && c != 223 && c != 248 && c != 254) { //Æ æ Ø Þ ß ø þ
    			c = map[(int)c - MIN ];
    		}
    		result[bcl] = c;
    	}
		
    	String res = new String(result);
		return res.toLowerCase();
	}
	
    /** Initialisation du tableau de correspondance entre les caractéres accentués
       * et leur homologues non accentués 
       */
    private static char[] initMap() {  
    	char[] map = new char[ MAX - MIN + 1 ];
    	char   car    = ' ';
	    car = 'A';
	    map[00] = car;            /* '\u00C0'   À   alt-0192  */  
	    map[01] = car;            /* '\u00C1'   Á   alt-0193  */
	    map[02] = car;            /* '\u00C2'   Â   alt-0194  */
	    map[03] = car;            /* '\u00C3'   Ã   alt-0195  */
	    map[04] = car;            /* '\u00C4'   Ä   alt-0196  */
	    map[05] = car;            /* '\u00C5'   Å   alt-0197  */
	    car = ' ';
	    map[06] = car;            /* '\u00C6'   Æ   alt-0198   ********* BI-CARACTERE ******** */
	    car = 'C';
	    map[07] = car;            /* '\u00C7'   Ç   alt-0199  */
	    car = 'E';
	    map[8]  = car;            /* '\u00C8'   È   alt-0200  */
	    map[9]  = car;            /* '\u00C9'   É   alt-0201  */
	    map[10] = car;            /* '\u00CA'   Ê   alt-0202  */
	    map[11] = car;            /* '\u00CB'   Ë   alt-0203  */
	    car = 'I';
	    map[12] = car;            /* '\u00CC'   Ì   alt-0204  */
	    map[13] = car;            /* '\u00CD'   Í   alt-0205  */
	    map[14] = car;            /* '\u00CE'   Î   alt-0206  */
	    map[15] = car;            /* '\u00CF'   Ï   alt-0207  */
	    car = 'D';
	    map[16] = car;            /* '\u00D0'   Ð   alt-0208  */
	    car = 'N';
	    map[17] = car;            /* '\u00D1'   Ñ   alt-0209  */
	    car = 'O';
	    map[18] = car;            /* '\u00D2'   Ò   alt-0210  */
	    map[19] = car;            /* '\u00D3'   Ó   alt-0211  */
	    map[20] = car;            /* '\u00D4'   Ô   alt-0212  */
	    map[21] = car;            /* '\u00D5'   Õ   alt-0213  */
	    map[22] = car;            /* '\u00D6'   Ö   alt-0214  */
	    car = '*';
	    map[23] = car;            /* '\u00D7'   ×   alt-0215            ***** NON ALPHA **** */
	    car = '0';
	    map[24] = car;            /* '\u00D8'   Ø   alt-0216  */
	    car = 'U';
	    map[25] = car;            /* '\u00D9'   Ù   alt-0217  */
	    map[26] = car;            /* '\u00DA'   Ú   alt-0218  */
	    map[27] = car;            /* '\u00DB'   Û   alt-0219  */
	    map[28] = car;            /* '\u00DC'   Ü   alt-0220  */
	    car = 'Y';
	    map[29] = car;            /* '\u00DD'   Ý   alt-0221  */
	    car = ' ';
	    map[30] = car;            /* '\u00DE'   Þ   alt-0222            ***** NON ALPHA **** */
	    car = 'B';
	    map[31] = car;            /* '\u00DF'   ß   alt-0223            ***** NON ALPHA **** */
	    car = 'a';
	    map[32] = car;            /* '\u00E0'   à   alt-0224  */
	    map[33] = car;            /* '\u00E1'   á   alt-0225  */
	    map[34] = car;            /* '\u00E2'   â   alt-0226  */
	    map[35] = car;            /* '\u00E3'   ã   alt-0227  */
	    map[36] = car;            /* '\u00E4'   ä   alt-0228  */
	    map[37] = car;            /* '\u00E5'   å   alt-0229  */
	    car = ' ';
	    map[38] = car;            /* '\u00E6'   æ   alt-0230            ********* BI-CARACTERE ******** */
	    car = 'c';
	    map[39] = car;            /* '\u00E7'   ç   alt-0231  */
	    car = 'e';
	    map[40] = car;            /* '\u00E8'   è   alt-0232  */
	    map[41] = car;            /* '\u00E9'   é   alt-0233  */
	    map[42] = car;            /* '\u00EA'   ê   alt-0234  */
	    map[43] = car;            /* '\u00EB'   ë   alt-0235  */
	    car = 'i';
	    map[44] = car;            /* '\u00EC'   ì   alt-0236  */
	    map[45] = car;            /* '\u00ED'   í   alt-0237  */
	    map[46] = car;            /* '\u00EE'   î   alt-0238  */
	    map[47] = car;            /* '\u00EF'   ï   alt-0239  */
	    car = 'd';
	    map[48] = car;            /* '\u00F0'   ð   alt-0240  */
	    car = 'n';
	    map[49] = car;            /* '\u00F1'   ñ   alt-0241  */
	    car = 'o';
	    map[50] = car;            /* '\u00F2'   ò   alt-0242  */
	    map[51] = car;            /* '\u00F3'   ó   alt-0243  */
	    map[52] = car;            /* '\u00F4'   ô   alt-0244  */
	    map[53] = car;            /* '\u00F5'   õ   alt-0245  */
	    map[54] = car;            /* '\u00F6'   ö   alt-0246  */
	    car = '/';
	    map[55] = car;            /* '\u00F7'   ÷   alt-0247            ***** NON ALPHA **** */
	    car = '0';
	    map[56] = car;            /* '\u00F8'   ø   alt-0248            ***** NON ALPHA **** */
	    car = 'u';
	    map[57] = car;            /* '\u00F9'   ù   alt-0249  */
	    map[58] = car;            /* '\u00FA'   ú   alt-0250  */
	    map[59] = car;            /* '\u00FB'   û   alt-0251  */
	    map[60] = car;            /* '\u00FC'   ü   alt-0252  */
	    car = 'y';
	    map[61] = car;            /* '\u00FD'   ý   alt-0253  */
	    car = ' ';
	    map[62] = car;            /* '\u00FE'   þ   alt-0254            ***** NON ALPHA **** */
	    car = 'y';
	    map[63] = car;            /* '\u00FF'   ÿ   alt-0255  */
      
       return map;
    }
}

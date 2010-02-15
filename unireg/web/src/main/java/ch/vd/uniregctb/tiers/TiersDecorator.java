package ch.vd.uniregctb.tiers;

import org.apache.log4j.Logger;
import org.displaytag.decorator.TableDecorator;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

public class TiersDecorator extends TableDecorator {

	protected final Logger LOGGER = Logger.getLogger(TiersDecorator.class);

	/**
	 * @return le nom courrier sur 2 lignes
	 */
    public String getNomCourrier() {

    	TiersIndexedData proxy = (TiersIndexedData) getCurrentRowObject();
    	String nomCourrier = "";
        if (proxy.getNom1() != null) {
        	nomCourrier =  nomCourrier + proxy.getNom1();
        }
        if (!nomCourrier.equals("")) {
        	nomCourrier =  nomCourrier + "<BR>";
        }
        if (proxy.getNom2() != null) {
        	nomCourrier =  nomCourrier + proxy.getNom2();
        }
        return nomCourrier;

    }

    public String getNumeroFormatte() {
    	TiersIndexedData tiers = (TiersIndexedData) getCurrentRowObject();
    	return FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero());
    }

}

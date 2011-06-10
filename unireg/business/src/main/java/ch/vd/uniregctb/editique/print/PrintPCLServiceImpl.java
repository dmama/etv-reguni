package ch.vd.uniregctb.editique.print;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.Sides;
import java.io.InputStream;

import ch.vd.securite.model.ProfilOperateur;

public class PrintPCLServiceImpl implements PrintPCLService {

	 @Override
	 public void printLocalStream(ProfilOperateur profilOperateur, InputStream inputStream) throws PrintPCLException {
		 final DocFlavor                 oFlavor = DocFlavor.INPUT_STREAM.AUTOSENSE;  			//defini un type de document
	     final Doc                       oDoc    = new SimpleDoc(inputStream, oFlavor, null); 	//defini le document a imprimer
	     final PrintRequestAttributeSet  oRset   = new HashPrintRequestAttributeSet();  		//defini des attributs de format de page
	     final PrintServiceAttributeSet  oSset   = new HashPrintServiceAttributeSet(); 			//defini l'imprimante sur laquelle on veux imprimer

	     oRset.add(new Copies(1));
	     oRset.add(MediaSizeName.ISO_A4);
	     oRset.add(Sides.DUPLEX);
	     oSset.add(new PrinterName(profilOperateur.getImprimante(), null));
	     final PrintService[] tabServices = PrintServiceLookup.lookupPrintServices(oFlavor, oSset);

	     if ( tabServices.length > 0 ) {
	        final DocPrintJob    oJob = tabServices[0].createPrintJob();
	        try {
	        	oJob.print(oDoc, oRset);
	        } catch (PrintException printException) {
	        	throw new PrintPCLException("Erreur lors de l'impression du document PCL", printException);
	        }
	     } else {
	    	 throw new PrintPCLException("Aucune imprimante ne correspond au nom :" + profilOperateur.getImprimante());
	     }
	 }

}

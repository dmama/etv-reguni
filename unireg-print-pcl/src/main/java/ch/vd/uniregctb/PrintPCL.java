package ch.vd.uniregctb;

import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

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
import javax.print.attribute.standard.PrinterName;

import org.apache.log4j.Logger;

/**
 * Classe qui ouvre la boîte de dialogue d'impression et permet à l'utilisateur d'imprimer directement le flux en PCL par exemple
 *
 * @author xcifde
 *
 */
public class PrintPCL implements Printable {

	private static final Logger LOGGER = Logger.getLogger(PrintPCL.class);

	private String nomImprimante;
	private static String cheminFichierTemporaire = "";

	public static void main(String[] args) {
		LOGGER.debug("Debut de l'impression");
		if (args != null && args.length > 0) {
			cheminFichierTemporaire = args[0];
		}
		PrintPCL printPCL = new PrintPCL();
		System.exit(0);
	}

	/**
	 * Constructeur: PrintPCL
	 * <p>
	 *
	 */
	public PrintPCL() {

		PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(this);

		// Affiche la boîte de dialogue d'impression
		// Lance la méthode print si on clicke sur 'OK'
		// Annule le job sinon
		if (printJob.printDialog()) {
			try {
				System.out.println("Nom imprimante:" + printJob.getPrintService().getName() + "--");
				nomImprimante = printJob.getPrintService().getName();
				printJob.print();
			}
			catch (Exception PrintException) {
				PrintException.printStackTrace();
			}
		}

	}

	/**
	 * Methode: print
	 * <p>
	 * Cette classe est responsable de l'impression en PCL
	 *
	 * @param g
	 * @param pageFormat
	 * @param page
	 *
	 * @return NO_SUCH_PAGE qui signifie d'arrêter l'impression
	 */
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		InputStream inputStream = null;

		try {
			inputStream = new FileInputStream(cheminFichierTemporaire);
		}
		catch (FileNotFoundException e) {
			LOGGER.error("Erreur au chargement du fichier --" + cheminFichierTemporaire + "--", e);
			System.exit(0);
		}

		final DocFlavor oFlavor = DocFlavor.INPUT_STREAM.AUTOSENSE; // defini un type de document
		final Doc oDoc = new SimpleDoc(inputStream, oFlavor, null); // defini le document a imprimer
		final PrintRequestAttributeSet oRset = new HashPrintRequestAttributeSet(); // defini des attributs de format de page
		final PrintServiceAttributeSet oSset = new HashPrintServiceAttributeSet(); // defini l'imprimante sur laquelle on veux imprimer

		oRset.add(new Copies(1));
		//oRset.add(MediaSizeName.ISO_A4);
		//oRset.add(Sides.DUPLEX);
		oSset.add(new PrinterName(nomImprimante, null));
		final PrintService[] tabServices = PrintServiceLookup.lookupPrintServices(oFlavor, oSset);

		if (tabServices.length > 0) {
			final DocPrintJob oJob = tabServices[0].createPrintJob();
			try {
				oJob.print(oDoc, oRset);
			}
			catch (PrintException printException) {
				LOGGER.error("Erreur à l'impression--", printException);
				System.exit(0);
			}
		}
		else {
			LOGGER.error("Erreur avec le nom de l'imprimante --" + nomImprimante + "--");
			System.exit(0);
		}

		LOGGER.debug("Fin de l'impression");
		return (NO_SUCH_PAGE);
	}
}

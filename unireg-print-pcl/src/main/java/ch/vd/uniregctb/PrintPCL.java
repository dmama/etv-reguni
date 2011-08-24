package ch.vd.uniregctb;

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
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.awt.print.PrinterJob;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * Classe qui ouvre la boîte de dialogue d'impression et permet à l'utilisateur d'imprimer directement le flux en PCL par exemple
 */
public class PrintPCL {

	private static final Logger LOGGER = Logger.getLogger(PrintPCL.class);

	private final String cheminFichierTemporaire;

	public static void main(String[] args) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Debut de l'impression");
		}

		final String cheminFichierTemporaire;
		if (args != null && args.length > 0) {
			cheminFichierTemporaire = args[0];
		}
		else {
			cheminFichierTemporaire = null;
		}

		try {
			final PrintPCL printPCL = new PrintPCL(cheminFichierTemporaire);
			printPCL.print();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Fin de l'impression");
			}

			System.exit(0);
		}
		catch (Exception e) {
			LOGGER.error("Erreur du traitement de l'impression", e);
			System.exit(1);
		}
	}

	/**
	 * @param cheminFichierTemporaire chemin du fichier contenant le flux PCL à imprimer
	 */
	public PrintPCL(String cheminFichierTemporaire) {
		this.cheminFichierTemporaire = cheminFichierTemporaire;
	}

	/**
	 * Point d'entrée de l'impression depuis la méthode {@link #main}
	 * @throws PrinterException en cas de souci à l'impression
	 * @throws IOException en cas de problème avec le fichier d'entrée
	 */
	private void print() throws PrinterException, IOException {

		final PrinterJob printJob = PrinterJob.getPrinterJob();

		// Affiche la boîte de dialogue d'impression
		// Lance la méthode print si on clicke sur 'OK'
		// Annule le job sinon
		if (printJob.printDialog()) {
			final String nomImprimante = printJob.getPrintService().getName();
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Nom imprimante : '%s'", nomImprimante));
			}
			printJob.setPrintable(new Job(nomImprimante, new FileInputStream(cheminFichierTemporaire)));
			printJob.print();
		}
	}

	/**
	 * Job d'impression
	 */
	private static class Job implements Printable {

		private final String nomImprimante;
		private final InputStream in;

		private Job(String nomImprimante, InputStream in) {
			this.nomImprimante = nomImprimante;
			this.in = in;
		}

		public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

			final DocFlavor oFlavor = DocFlavor.INPUT_STREAM.AUTOSENSE;                 // definit un type de document
			final Doc oDoc = new SimpleDoc(in, oFlavor, null);                          // definit le document a imprimer
			final PrintRequestAttributeSet oRset = new HashPrintRequestAttributeSet();  // definit des attributs de format de page
			final PrintServiceAttributeSet oSset = new HashPrintServiceAttributeSet();  // definit l'imprimante sur laquelle on veux imprimer

			oRset.add(new Copies(1));
			oSset.add(new PrinterName(nomImprimante, null));
			final PrintService[] tabServices = PrintServiceLookup.lookupPrintServices(oFlavor, oSset);

			if (tabServices.length > 0) {
				final DocPrintJob oJob = tabServices[0].createPrintJob();
				try {
					oJob.print(oDoc, oRset);
				}
				catch (PrintException e) {
					LOGGER.error("Erreur à l'impression", e);
					throw new PrinterAbortException(e.getMessage());
				}
			}
			else {
				final String msg = String.format("Impossible de trouver l'imprimante '%s'", nomImprimante);
				LOGGER.error(msg);
				throw new PrinterIOException(new IOException(msg));
			}

			// pour une raison que je ne comprends pas, si je mets autre chose
			// que ça, je choppe une page A4 blanche en fin d'impression...
			return NO_SUCH_PAGE;
		}
	}
}

package ch.vd.uniregctb.utils;

import java.io.FileWriter;

import ch.vd.registre.fiscal.model.Assujettissement;
import ch.vd.registre.fiscal.model.ContribuableFoyer;
import ch.vd.registre.fiscal.model.DeclarationImpot;
import ch.vd.registre.fiscal.model.For;
import ch.vd.registre.base.date.RegDate;

import com.thoughtworks.xstream.XStream;

public class ContribuableComparator {

	XStream xstream = new XStream();
	private static String CONTRIBUABLE_IDENTIQUES = "Ifonrmations de base du contribuables identiques";
	private static String ASSUJETISSEMENT_IDENTIQUES = "Assujetissements identiques        ";
	private static String DECLARATION_IDENTIQUES = "Declarations d'impots identiques        ";
	private static String FOR_IDENTIQUES = "Derniers fors identiques        ";
	private static String ADRESSE_IDENTIQUES = "Adresse identiques        ";

	public void compare(ContribuableFoyer contribuableHost, ContribuableFoyer contribuableUnireg) throws Exception {

		 FileWriter writerResultat = new FileWriter("resultatComparaison" + contribuableHost.getNoContribuable() + ".txt");
		//FileWriter writerResultat = new FileWriter("resultatComparaison" + ".txt", true);
		String message = CONTRIBUABLE_IDENTIQUES;

		writerResultat.write("                   -----------------------------------------------------\n");
		writerResultat.write("                             DIFFERENCES  ENTRE HOST et UNIREG \n");
		writerResultat.write("                               POUR LE CONTRIBUABLE " + contribuableHost.getNoContribuable() + "\n");
		writerResultat.write("                   -----------------------------------------------------\n");

		if (contribuableHost != null) {
			if (contribuableUnireg != null) {
				if (contribuableHost.getNoContribuable() != contribuableUnireg.getNoContribuable()) {
					message = "le numéro du contribuable:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + contribuableHost.getNoContribuable() + " |  Unireg => "
							+ contribuableUnireg.getNoContribuable() + "\n");

					writerResultat.write("\n");
					if (contribuableHost.getPrincipal().getNoContribuable() != contribuableUnireg.getPrincipal().getNoContribuable()) {
						message = "le numéro du contribuable Principal:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + contribuableHost.getPrincipal().getNoContribuable() + " |  Unireg => "
								+ contribuableUnireg.getPrincipal().getNoContribuable() + "\n");

						writerResultat.write("\n");
					}

				}

				if (contribuableHost.getTypeContribuable() != null && contribuableUnireg.getTypeContribuable() != null) {

					if (!contribuableHost.getTypeContribuable().equals(contribuableUnireg.getTypeContribuable())) {
						message = "le type de contribuable:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + contribuableHost.getTypeContribuable().getName() + " |  Unireg => "
								+ contribuableUnireg.getTypeContribuable().getName() + "\n");
						writerResultat.write("\n");
					}
				}
				if (!contribuableHost.getFormuleDePolitesse().equals(contribuableUnireg.getFormuleDePolitesse())) {
					message = "la formule de politesse:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + contribuableHost.getFormuleDePolitesse() + " |  Unireg => "
							+ contribuableUnireg.getFormuleDePolitesse() + "\n");
					writerResultat.write("\n");
				}

				if (!contribuableHost.getNomCourrier1().equals(contribuableUnireg.getNomCourrier1())) {
					message = "le nom courrier 1:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + contribuableHost.getNomCourrier1() + " |  Unireg => "
							+ contribuableUnireg.getNomCourrier1() + "\n");
					writerResultat.write("\n");
				}

				if (contribuableHost.getNomCourrier2() != null) {

					if (!contribuableHost.getNomCourrier2().equals(contribuableUnireg.getNomCourrier2())) {
						message = "le nom courrier 2:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + contribuableHost.getNomCourrier2() + " |  Unireg => "
								+ contribuableUnireg.getNomCourrier2() + "\n");
						writerResultat.write("\n");
					}
				}

				if (contribuableHost.getCodeBlocageRmbtAuto() != contribuableUnireg.getCodeBlocageRmbtAuto()) {
					message = "le code de blocage du remboursement automatique:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + contribuableHost.getCodeBlocageRmbtAuto() + " |  Unireg => "
							+ contribuableUnireg.getCodeBlocageRmbtAuto() + "\n");
					writerResultat.write("\n");
				}

				if (contribuableHost.getNumeroTelephoniqueFixe() != null) {
					if (!contribuableHost.getNumeroTelephoniqueFixe().equals(contribuableUnireg.getNumeroTelephoniqueFixe())) {
						message = "le numero de téléphone fixe:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + contribuableHost.getNumeroTelephoniqueFixe() + " |  Unireg => "
								+ contribuableUnireg.getNumeroTelephoniqueFixe() + "\n");
						writerResultat.write("\n");
					}

				}

				if (contribuableHost.getNumeroTelephoniquePortable() != null) {
					if (!contribuableHost.getNumeroTelephoniquePortable().equals(contribuableUnireg.getNumeroTelephoniquePortable())) {
						message = "le numero de téléphone portable:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + contribuableHost.getNumeroTelephoniquePortable() + " |  Unireg => "
								+ contribuableUnireg.getNumeroTelephoniquePortable() + "\n");
						writerResultat.write("\n");
					}

				}

				if (contribuableHost.getEmail() != null) {
					if (!contribuableHost.getEmail().equals(contribuableUnireg.getEmail())) {
						message = "l'email:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + contribuableHost.getEmail() + " |  Unireg => " + contribuableUnireg.getEmail()
								+ "\n");
						writerResultat.write("\n");
					}
				}

				if (message.equals(CONTRIBUABLE_IDENTIQUES)) {
					writerResultat.write(message + "\n");
				}
				/** Adresse */

				message = ADRESSE_IDENTIQUES;

				if (!compareContribuableObject(contribuableHost.getAdresse(), contribuableUnireg.getAdresse())) {

					writerResultat.write("-----------------------------------------------------\n");
					writerResultat.write("        COMPARAISON DES ADRESSES \n");
					writerResultat.write("-----------------------------------------------------\n");

					message = "l'adresse du contribuable:";
					writerResultat.write(message + "\n");
					if (contribuableHost.getAdresse() != null) {
						writerResultat.write("Adresse Host" + "\n");
						writerResultat.write(xstream.toXML(contribuableHost.getAdresse()) + "\n");
						if (contribuableUnireg.getAdresse() != null) {
							writerResultat.write("Adresse Unireg" + "\n");
							writerResultat.write(xstream.toXML(contribuableUnireg.getAdresse()) + "\n");
						}
						else {
							writerResultat.write("l'adresse est nulle pour le contribuable Unireg " + "\n");
						}
					}

				}
				if (message.equals(ADRESSE_IDENTIQUES)) {
					writerResultat.write(message + "\n");
				}
				if (contribuableUnireg.getPrincipal() != null && contribuableHost.getPrincipal() != null) {
					if (contribuableHost.getPrincipal().getNoContribuable() != contribuableUnireg.getPrincipal().getNoContribuable()) {
						message = "le contribuable principal:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + contribuableHost.getPrincipal().getNoContribuable() + " |  Unireg => "
								+ contribuableUnireg.getPrincipal().getNoContribuable() + "\n");

						writerResultat.write("\n");
					}
				}

				if (contribuableUnireg.getDernierAssujettissement() != null) {
					compareAssujetissement(contribuableHost.getDernierAssujettissement(), contribuableUnireg.getDernierAssujettissement(),
							writerResultat);
				}
				if (contribuableUnireg.getDernierFor() != null) {
					compareFor(contribuableHost.getDernierFor(), contribuableUnireg.getDernierFor(), writerResultat);
				}
				writerResultat.close();
			}
			else {
				writerResultat.write("le contribuable unireg est null " + "\n");
			}
		}
	}

	private boolean compareContribuableObject(Object host, Object unireg) {

		return xstream.toXML(host).toLowerCase().equals(xstream.toXML(unireg).toLowerCase());

	}

	public void compareAssujetissement(Assujettissement asHost, Assujettissement asUnireg, FileWriter writerResultat) throws Exception {
		String message = ASSUJETISSEMENT_IDENTIQUES;

		if (asHost != null) {
			writerResultat.write("-----------------------------------------------------\n");
			writerResultat.write("        COMPARAISON DU DERNIER ASSUJETISSEMENT \n");
			writerResultat.write("-----------------------------------------------------\n");

			RegDate debutHost = RegDate.get(asHost.getDateDebut());
			RegDate finHost = RegDate.get(asHost.getDateFin());
			RegDate debutUnireg = RegDate.get(asUnireg.getDateDebut());
			RegDate finUnireg = RegDate.get(asUnireg.getDateFin());

			if (asUnireg != null) {
				debutUnireg = RegDate.get(asUnireg.getDateDebut());
				finUnireg = RegDate.get(asUnireg.getDateFin());
			}
			else {
				debutUnireg = RegDate.getEarlyDate();
				finUnireg = RegDate.getLateDate();
			}

			if (debutHost != null && debutHost.compareTo(debutUnireg) != 0) {
				message = "Date de debut:";
				writerResultat.write(message + "\n");
				writerResultat.write(" Host => " + debutHost.toString() + " |  Unireg => " + debutUnireg.toString() + "\n");
				writerResultat.write("\n");
			}

			if (finHost != null && finHost.compareTo(finUnireg) != 0) {
				message = "Date de fin:";
				writerResultat.write(message + "\n");
				writerResultat.write(" Host => " + finHost.toString() + " |  Unireg => " + finUnireg.toString() + "\n");
				writerResultat.write("\n");
			}

			if (asHost.getNoSequence() != asUnireg.getNoSequence()) {
				message = "Numero de sequence:";
				writerResultat.write(message + "\n");
				writerResultat.write(" Host => " + asHost.getNoSequence() + " |  Unireg => " + asUnireg.getNoSequence() + "\n");
				writerResultat.write("\n");
			}

			if (!asHost.getTypeAssujettissement().equals(asUnireg.getTypeAssujettissement())) {
				message = "Type d'assujetissement:";
				writerResultat.write(message + "\n");
				writerResultat.write(" Host => " + asHost.getTypeAssujettissement() + " |  Unireg => " + asUnireg.getTypeAssujettissement()
						+ "\n");
				writerResultat.write("\n");
			}
			if (message.equals(ASSUJETISSEMENT_IDENTIQUES)) {
				writerResultat.write(message + "\n");
			}

			compareDeclarationImpot(asHost.getDerniereDeclarationImpot(), asUnireg.getDerniereDeclarationImpot(), writerResultat);
		}
	}

	public void compareDeclarationImpot(DeclarationImpot declarationHost, DeclarationImpot declarationUnireg, FileWriter writerResultat)
			throws Exception {
		String message = DECLARATION_IDENTIQUES;
		if (declarationHost != null) {

			if (declarationUnireg != null) {
				writerResultat.write("-----------------------------------------------------\n");
				writerResultat.write("   COMPARAISON DE LA DERNIERE DECLARATION D'IMPOT   \n");
				writerResultat.write("-----------------------------------------------------\n");

				RegDate dateDernierEtatHost = RegDate.get(declarationHost.getDateDernierEtat());
				RegDate dateDernierEtatUnireg = RegDate.get(declarationUnireg.getDateDernierEtat());

				RegDate dateEcheanceHost = RegDate.get(declarationHost.getDateEcheance());
				RegDate dateEcheanceUnireg = RegDate.get(declarationUnireg.getDateEcheance());

				RegDate dateEnvoiHost = RegDate.get(declarationHost.getDateEnvoi());
				RegDate dateEnvoiUnireg = RegDate.get(declarationUnireg.getDateEnvoi());

				RegDate dateSommationHost = RegDate.get(declarationHost.getDateSommation());
				RegDate dateSommationUnireg = RegDate.get(declarationUnireg.getDateSommation());

				if (dateDernierEtatHost != null && dateDernierEtatHost.compareTo(dateDernierEtatUnireg) != 0) {
					message = "Date du dernier etat:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + dateDernierEtatHost.toString() + " |  Unireg => " + dateDernierEtatUnireg.toString()
							+ "\n");
					writerResultat.write("\n");
				}

				if (dateEcheanceHost != null && dateEcheanceHost.compareTo(dateEcheanceUnireg) != 0) {
					message = "Date d'echeance:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + dateEcheanceHost.toString() + " |  Unireg => " + dateEcheanceUnireg.toString()
							+ "\n");
					writerResultat.write("\n");
				}

				if (dateEnvoiHost != null && dateEnvoiHost.compareTo(dateEnvoiUnireg) != 0) {
					message = "Date d'envoi";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + dateEnvoiHost.toString() + " |  Unireg => " + dateEnvoiUnireg.toString() + "\n");
					writerResultat.write("\n");
				}

				if (dateSommationHost != null && dateSommationHost.compareTo(dateSommationUnireg) != 0) {
					message = "Date de sommation:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + dateSommationHost.toString() + " |  Unireg => " + dateSommationUnireg.toString()
							+ "\n");
					writerResultat.write("\n");
				}

				if (declarationHost.getPeriodeFiscale() != declarationUnireg.getPeriodeFiscale()) {
					message = "Période fiscale:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + declarationHost.getPeriodeFiscale() + " |  Unireg => "
							+ declarationUnireg.getPeriodeFiscale() + "\n");
					writerResultat.write("\n");
				}

				if (!declarationHost.getDernierEtatDeclarationImpot().equals(declarationUnireg.getDernierEtatDeclarationImpot())) {
					message = "Dernier etat:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + declarationHost.getDernierEtatDeclarationImpot().getName() + " |  Unireg => "
							+ declarationUnireg.getDernierEtatDeclarationImpot().getName() + "\n");
					writerResultat.write("\n");
				}
				if (message.equals(DECLARATION_IDENTIQUES)) {
					writerResultat.write(message + "\n");
				}
			}
		}
		else {
			writerResultat.write("La declaration unireg est nulle" + "\n");
		}

	}

	public void compareFor(For forHost, For forUnireg, FileWriter writerResultat) throws Exception {

		String message = FOR_IDENTIQUES;
		if (forHost != null) {
			if (forUnireg != null) {
				writerResultat.write("-----------------------------------------------------\n");
				writerResultat.write("             COMPARAISON DU DERNIER FOR              \n");
				writerResultat.write("-----------------------------------------------------\n");

				RegDate debutHost = RegDate.get(forHost.getDateDebutValidite());
				RegDate finHost = RegDate.get(forHost.getDateFinValidite());
				RegDate debutUnireg = RegDate.get(forUnireg.getDateDebutValidite());
				RegDate finUnireg = RegDate.get(forUnireg.getDateFinValidite());

				if (forHost.getCommune() != null) {
					if (forUnireg.getCommune() != null) {
						if (forHost.getCommune().getNoOFS() != forUnireg.getCommune().getNoOFS()) {
							message = "Commune:";
							writerResultat.write(message + "\n");
							writerResultat.write(" Host => " + forHost.getCommune().getNomMajuscule() + " |  Unireg => "
									+ forUnireg.getCommune().getNomMajuscule() + "\n");
							writerResultat.write("\n");
						}
					}
					else {
						writerResultat.write("la commune du for est null pour le contribuable Unireg Host=> "
								+ forHost.getCommune().getNomMinuscule() + "\n");
					}
				}

				if (debutHost != null && debutHost.compareTo(debutUnireg) != 0) {
					message = "Date de debut de validité:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + debutHost.toString() + " |  Unireg => " + debutUnireg.toString() + "\n");
					writerResultat.write("\n");
				}

				if (finHost != null && finHost.compareTo(finUnireg) != 0) {
					message = "Date de fin validité:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + finHost.toString() + " |  Unireg => " + finUnireg.toString() + "\n");
					writerResultat.write("\n");
				}

				if (forHost.getMotifDebut() != null) {

					if (!forHost.getMotifDebut().equals(forUnireg.getMotifDebut())) {
						message = "Motif d'ouverture:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + forHost.getMotifDebut() + " |  Unireg => " + forUnireg.getMotifDebut() + "\n");
						writerResultat.write("\n");
					}
				}
				if (forHost.getMotifFin() != null) {

					if (!forHost.getMotifFin().equals(forUnireg.getMotifFin())) {
						message = "Motif de fermeture:";
						writerResultat.write(message + "\n");
						writerResultat.write(" Host => " + forHost.getMotifFin() + " |  Unireg => " + forUnireg.getMotifFin() + "\n");
						writerResultat.write("\n");
					}
				}

				if (forHost.getNoSequence() != forUnireg.getNoSequence()) {
					message = "Numero de sequence:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + forHost.getNoSequence() + " |  Unireg => " + forUnireg.getNoSequence() + "\n");
					writerResultat.write("\n");
				}

				if (forHost.getPays() != null) {
					if (forUnireg.getPays() != null) {
						if (forHost.getPays().getNoOFS() != forUnireg.getPays().getNoOFS()) {
							message = "Pays:";
							writerResultat.write(message + "\n");
							writerResultat.write(" Host => " + forHost.getPays().getNomMajuscule() + " |  Unireg => "
									+ forUnireg.getPays().getNomMajuscule() + "\n");
							writerResultat.write("\n");
						}
					}
					else {
						writerResultat.write("le pays du for est null pour le contribuable Unireg. Host => "
								+ forHost.getPays().getNomMinuscule() + "\n");
					}
				}

				if (!forHost.getTypeFor().getName().equals(forUnireg.getTypeFor().getName())) {
					message = "Type de for:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + forHost.getTypeFor().getName() + " |  Unireg => " + forUnireg.getTypeFor().getName()
							+ "\n");
					writerResultat.write("\n");
				}

				if (forHost.isForGestion() != forUnireg.isForGestion()) {
					message = "For de gestion:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + forHost.isForGestion() + " |  Unireg => " + forUnireg.isForGestion() + "\n");
					writerResultat.write("\n");
				}

				if (forHost.isSejourPourActivite() != forUnireg.isSejourPourActivite()) {
					message = "Sejour pour activite:";
					writerResultat.write(message + "\n");
					writerResultat.write(" Host => " + forHost.isSejourPourActivite() + " |  Unireg => " + forUnireg.isSejourPourActivite()
							+ "\n");
					writerResultat.write("\n");
				}
				if (message.equals(FOR_IDENTIQUES)) {
					writerResultat.write(message + "\n");
				}
			}
		}
		else {
			writerResultat.write("Le for Unireg est null" + "\n");
		}
	}
}

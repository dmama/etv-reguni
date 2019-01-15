package ch.vd.unireg.di.manager;


import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.ModeleFeuilleDocumentEditique;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeDocument;

/**
 * Service offrant des methodes pour gérer le controller DeclarationImpotEditController
 *
 * @author xcifde
 */
public interface DeclarationImpotEditManager {

	String CANNOT_ADD_NEW_DI = "Le contribuable n'est pas assujetti, ou toutes ses déclarations sont déjà créées.";

	/**
	 * [UNIREG-832] Calcule les dates de début et de fin pour la création de la prochaine d'impôt sur un contribuable. Si plusieurs déclarations n'ont pas été envoyées durant les années précédentes,
	 * cette méthode retourne les dates de la déclaration non-envoyée la plus ancienne.
	 * <p/>
	 * <b>Note:</b> Si le contribuable n'est pas du tout assujetti, ou s'il n'est plus assujetti et que toutes ses déclarations ont été envoyées, la méthode retourne <code>null</code>.
	 *
	 * @param numero le numéro de contribuable
	 * @return une liste de périodes d'imposition à utiliser pour une nouvelle déclaration, <code>null</code> s'il n'est pas possible d'ajouter une déclaration d'impôt.
	 * @throws ValidationException si le contribuable ne valide pas, ou s'il n'est pas possible de déterminer son assujettissement.
	 */
	List<PeriodeImposition> calculateRangesProchainesDIs(Long numero) throws ValidationException;

	/**
	 * Crée, sauve en base et imprime une DI vierge, ou imprime un duplicat de DI existante.
	 *
	 * @param ctbId le numéro de contribuable concerné
	 * @param dateDebut la date de début de validité de la déclaration
	 * @param dateFin la date de fin de validité de la déclaration
	 * @param typeDocument le type de document de la déclaration
	 * @param adresseRetour l'adresse de retour de la déclaration
	 * @param delaiAccorde le délais accordé
	 * @param dateRetour la date de retour si la déclaration a déjà été retournée
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat envoieImpressionLocaleDI(Long ctbId, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument, TypeAdresseRetour adresseRetour,
	                                          RegDate delaiAccorde, @Nullable RegDate dateRetour) throws Exception;

	@Transactional(rollbackFor = Throwable.class)
	void genererDISansImpression(Long ctbId, RegDate dateDebut, RegDate dateFin, RegDate delaiAccorde, @Nullable RegDate dateRetour) throws Exception;

	/**
	 * Persiste en base la nouvelle demande de delai
	 */
	Long saveNouveauDelai(Long idDeclaration, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal etat, boolean sursis);

	/**
	 * Persiste en base une modification d'une demande de délai existante
	 * @param idDelai identifiant du délai
	 * @param etat nouvel état de la demande de délai
	 * @param delaiAccordeAu nouvelle date de délai accordé
	 */
	void saveDelai(Long idDelai, EtatDelaiDocumentFiscal etat, RegDate delaiAccordeAu);

	/**
	 * Sommer une déclaration d'impôt
	 *
	 * @param id l'id de la déclaration d'impôt à sommer.
	 */
	EditiqueResultat envoieImpressionLocalSommationDI(Long id) throws EditiqueException, DeclarationException;

	/**
	 * Imprimer la lettre de confirmation ou de refus d'un délai de déclaration PP
	 */
	EditiqueResultat envoieImpressionLocalLettreDecisionDelaiPP(Long idDI, Long idDelai) throws EditiqueException;
	void envoieImpressionBatchLettreDecisionDelaiPP(Long idDelai) throws EditiqueException;

	/**
	 * Imprimer la lettre de confirmation ou de refus d'un délai de déclaration PM
	 */
	EditiqueResultat envoieImpressionLocaleLettreDecisionDelaiPM(Long idDelai) throws EditiqueException;
	void envoieImpressionBatchLettreDecisionDelaiPM(Long idDelai) throws EditiqueException;

	EditiqueResultat envoieImpressionLocalDuplicataDI(Long id, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes, boolean saveModele) throws DeclarationException;

	/**
	 * [UNIREG-832] Vérifie que les dates de début et de fin pour la création d'une déclaration d'impôt sont correctes.
	 *
	 * @param contribuable le contribuable
	 * @param range        le range de de validité de la déclaration à créer.
	 * @throws ValidationException si le contribuable ne valide pas, n'est pas du tout assujetti, si les dates ne correspondent pas à l'assujettissement calculé ou s'il existe déjà une déclaration.
	 */
	PeriodeImposition checkRangeDi(Contribuable contribuable, DateRange range) throws ValidationException;

	/**
	 * [UNIREG-832] Vérifie que les dates de début et de fin pour la création d'une déclaration d'impôt sont correctes.
	 *
	 * @param contribuable le contribuable
	 * @param range        le range de de validité de la déclaration à créer.
	 * @throws ValidationException si le contribuable ne valide pas, n'est pas du tout assujetti, si les dates ne correspondent pas à l'assujettissement calculé ou s'il existe déjà une déclaration.
	 */
	PeriodeImpositionPersonnesPhysiques checkRangeDi(ContribuableImpositionPersonnesPhysiques contribuable, DateRange range) throws ValidationException;

	/**
	 * Vérifie que les dates de début et de fin pour la création d'une déclaration d'impôt sont correctes.
	 *
	 * @param contribuable le contribuable
	 * @param range        le range de de validité de la déclaration à créer.
	 * @throws ValidationException si le contribuable ne valide pas, n'est pas du tout assujetti, si les dates ne correspondent pas à l'assujettissement calculé ou s'il existe déjà une déclaration.
	 */
	PeriodeImpositionPersonnesMorales checkRangeDi(ContribuableImpositionPersonnesMorales contribuable, DateRange range) throws ValidationException;

	/**
	 * Quittancer (= ajout un état 'retourné') manuellement une déclaration.
	 *
	 * @param id           l'id de la déclaration à quittancer
	 * @param typeDocument le type de document retourné
	 * @param dateRetour   la date de retour de la déclaration
	 * @return la déclaration quittancée
	 */
	DeclarationImpotOrdinaire quittancerDI(long id, TypeDocument typeDocument, RegDate dateRetour);
}


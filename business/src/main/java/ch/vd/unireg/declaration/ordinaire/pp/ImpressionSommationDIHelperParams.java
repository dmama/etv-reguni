package ch.vd.unireg.declaration.ordinaire.pp;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;

/**
 * Classe servant à stocker les paramètres à passer à {@link ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper#remplitSommationDI(ImpressionSommationDIHelperParams)}
 * 
 * @author xsifnr
 *
 */
public class ImpressionSommationDIHelperParams {
	
	private final boolean batch;
	private final DeclarationImpotOrdinairePP di;
	private final RegDate dateTraitement;
	private final boolean miseSousPliImpossible;
	private final String traitePar;
	private final String adrMsg;
	private final String tel;
	private final Integer montantEmolument;

	private ImpressionSommationDIHelperParams(boolean batch, DeclarationImpotOrdinairePP di, RegDate dateTraitement, boolean miseSousPliImpossible, String traitePar, String adrMsg, String tel, Integer montantEmolument) {
		this.batch = batch;
		this.di = di;
		this.dateTraitement = dateTraitement;
		this.miseSousPliImpossible = miseSousPliImpossible;
		this.traitePar = traitePar;
		this.adrMsg = adrMsg;
		this.tel = tel;
		this.montantEmolument = montantEmolument;
	}

	/**
	 * @return true si les parametres sont utilisés dans le cadre d'une impression demandée par le batch des sommations
	 */
	public boolean isBatch() {
		return batch;
	}
	
	/**
	 * @return true si les parametres sont utilisés dans le cadre d'une impression demandée online par un utilisateur d'UNIREG
	 */
	public boolean isOnline() {
		return !batch;
	}

	/**
	 * @return la di sur laquelle porte la sommation
	 */
	public DeclarationImpotOrdinairePP getDi() {
		return di;
	}

	/**
	 * @return la date à laquelle la DI est sommé
	 */
	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	/**
	 * @return true si le batch des sommations de DI est lancé avec ce paramètre à true (false sinon)
	 */
	public boolean isMiseSousPliImpossible() {
		return miseSousPliImpossible;
	}

	/**
	 * @return valeur à afficher dans le champ "traité par" (CAT pour le batch, Prénom + Nom pour le online)
	 */
	public String getTraitePar() {
		return traitePar;
	}
	
	/**
	 * @return valeur de l'adresse email de l'operateur traitant la sommation dans le cas d'une sommation online
	 */
	public String getAdrMsg() {
		return adrMsg;
	}
	
	/**
	 * @return valeur du no de tel de l'operateur traitant la sommation dans le cas d'une sommation online, du CAT pour un traitement batch
	 */
	public String getNoTelephone(){
		return tel;
	}

	/**
	 * @return montant de l'émolument à percevoir pour la sommation
	 */
	@Nullable
	public Integer getMontantEmolument() {
		return montantEmolument;
	}

	/**
	 * Instancie un nouvel objet {@link ImpressionSommationDIHelperParams} à utiliser 
	 * lors d'un impression online de la sommation
	 * 
	 * @param di
	 * @param traitePar
	 * @param adrMsg
	 * @param tel
	 * @param dateTraitement
	 * @return
	 */
	public static ImpressionSommationDIHelperParams online(DeclarationImpotOrdinairePP di, String traitePar, String adrMsg, String tel, RegDate dateTraitement, @Nullable Integer montantEmolument) {
		return new ImpressionSommationDIHelperParams(false, di, dateTraitement, false, traitePar, adrMsg, tel, montantEmolument);
	}
	
	/**
	 * 
	 * Instancie un nouvel objet {@link ImpressionSommationDIHelperParams} à utiliser 
	 * lors d'un impression online de la sommation
	 * 
	 * @param di
	 * @param miseSousPliImpossible
	 * @param dateTraitement
	 * @return
	 */
	public static ImpressionSommationDIHelperParams batch(DeclarationImpotOrdinairePP di, boolean miseSousPliImpossible, RegDate dateTraitement, @Nullable Integer montantEmolument) {
		return new ImpressionSommationDIHelperParams(true, di, dateTraitement, miseSousPliImpossible, "CAT", null, null, montantEmolument);
	}
}

package ch.vd.uniregctb.declaration.ordinaire;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

/**
 * Classe servant à stocker les paramètres à passer à {@link ImpressionSommationDIHelper#remplitSommationDI(ImpressionSommationDIHelperParams)}
 * 
 * @author xsifnr
 *
 */
public class ImpressionSommationDIHelperParams {
	
	private boolean batch;
	private DeclarationImpotOrdinaire di;
	private RegDate dateTraitement;
	private boolean miseSousPliImpossible;
	private String traitePar;
	private String adrMsg;
	private String tel;
	
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
	public DeclarationImpotOrdinaire getDi() {
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
	
	private ImpressionSommationDIHelperParams(){};
	
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
	static public ImpressionSommationDIHelperParams createOnlineParams(DeclarationImpotOrdinaire di, String traitePar, String adrMsg, String tel, RegDate dateTraitement) {
		ImpressionSommationDIHelperParams params = new ImpressionSommationDIHelperParams();
		params.batch = false;
		params.di = di;
		params.miseSousPliImpossible = false;
		params.dateTraitement = dateTraitement;
		params.traitePar = traitePar;
		params.adrMsg = adrMsg;
		params.tel = tel;
		return params;
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
	static public ImpressionSommationDIHelperParams createBatchParams(
			DeclarationImpotOrdinaire di, 
			boolean miseSousPliImpossible,
			RegDate dateTraitement
	) {
		ImpressionSommationDIHelperParams params = new ImpressionSommationDIHelperParams();
		params.batch = true;
		params.di = di;
		params.miseSousPliImpossible = miseSousPliImpossible;
		params.dateTraitement = dateTraitement;
		params.traitePar = "CAT";
		params.adrMsg = null;		
		params.tel = null; // le numero de telephone pour les impressions générées par batch est le numero de l'aci. il est renseigné lors de la construction du xml.
		return params;
	}



}

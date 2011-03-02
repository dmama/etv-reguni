package ch.vd.uniregctb.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;

/**
 * Interface implémentée par les extracteurs qui supportent le traitement par lot
 * @param <E> classe des éléments qui composent les lots en entrée
 * @param <R> classe du container des résultats
 */
public interface BatchableExtractor<E, R extends BatchResults<E, R>> extends Extractor {

	/**
	 * Instancie un rapport qui sera utilisé pour aggréger toutes les données de chacun des petits lots (rapport final) ou pour chacun des lots intermédiaires
	 * @param rapportFinal <code>true</code> s'il s'agit du rapport final, <code>false</code> s'il s'agit d'un rapport intermédiaire
	 * @return le rapport créé
	 */
	R createRapport(boolean rapportFinal);

	/**
	 * @return le comportement à adopter en cas d'erreur dans un lot
	 */
	BatchTransactionTemplate.Behavior getBatchBehavior();

	/**
	 * Méthode appelée dans le cadre d'une transaction read-only
	 * @return la liste des éléments qui seront ensuite découpés en lots
	 */
	List<E> buildElementList();

	/**
	 * @return la taille d'un lot
	 */
	int getBatchSize();

	/**
	 * Méthode appelée dans le cadre d'une transaction par le {@link BatchTransactionTemplate}
	 * @param batch le lot à traiter
	 * @param rapport le rapport à remplir à partir des informations du lot
	 * @return <code>true</code> s'il faut continuer avec le lot suivant, <code>false</code> si le traitement doit s'arrêter là
	 * @throws Exception en cas de problème ; en fonction du comportement (voir {@link #getBatchBehavior()}) demandé, une reprise pourra être entreprise ou pas
	 */
	boolean doBatchExtraction(List<E> batch, R rapport) throws Exception;

	/**
	 * Notification de l'avancement de l'extraction, appelé après le commit de chaque lot
	 * @param rapportFinal rapport final aggrégé avec tous les lots traités jusque là
	 * @param percentProgression pourcentage d'avancement du traitement des lots
	 */
	void afterTransactionCommit(R rapportFinal, int percentProgression);

	/**
	 * @param rapportFinal rapport aggrégé de tous les lots
	 * @return un flux en lecture sur la donnée brute de l'extraction (= le contenu du fichier CSV, par exemple)
	 * @throws IOException en cas de problème avec le flux
	 */
	InputStream getStreamForExtraction(R rapportFinal) throws IOException;

	/**
	 * @return le MIME-type du document renvoyé dans le stream
	 */
	String getMimeType();
}

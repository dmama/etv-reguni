package ch.vd.unireg.scheduler;

import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.SchedulerException;

/**
 * Interface implémentée par le batch scheduler
 */
public interface BatchScheduler {

	/**
	 * @return <code>true</code> si le scheduler est démarré
	 * @throws SchedulerException en cas de problème
	 */
	boolean isStarted() throws SchedulerException;

	/**
	 * Enregistrement d'un nouveau job
	 * @param jobDef définition du job à enregistrer
	 * @throws SchedulerException en cas de problème
	 */
	void register(JobDefinition jobDef) throws SchedulerException;

	/**
	 * Enregistre un job comme devant être exécuté comme un cron.
	 *
	 * @param job            un job
	 * @param cronExpression l'expression cron (par exemple: "0 0/5 6-20 * * ?" pour exécuter le job toutes les 5 minutes, de 6h à 20h tous les jours)
	 * @throws SchedulerException en cas d'exception dans le scheduler
	 * @throws java.text.ParseException     en cas d'erreur dans la syntaxe de l'expression cron
	 */
	void registerCron(JobDefinition job, String cronExpression) throws SchedulerException, ParseException;

	/**
	 * Enregistre un job comme devant être exécuté comme un cron.
	 *
	 * @param job            un job
	 * @param params         les paramètres de démarrage du job
	 * @param cronExpression l'expression cron (par exemple: "0 0/5 6-20 * * ?" pour exécuter le job toutes les 5 minutes, de 6h à 20h tous les jours)
	 * @throws SchedulerException en cas d'exception dans le scheduler
	 * @throws ParseException     en cas d'erreur dans la syntaxe de l'expression cron
	 */
	void registerCron(JobDefinition job, @Nullable Map<String, Object> params, String cronExpression) throws SchedulerException, ParseException;

	/**
	 * Démarre l'exécution d'un job avec les paramètres spécifiés.
	 *
	 * @param jobName le nom du job à démarrer
	 * @param params  les paramètres du job
	 * @return la définition du job
	 * @throws SchedulerException         en cas d'erreur de scheduling Quartz
	 * @throws JobAlreadyStartedException si le job est déjà démarré
	 */
	JobDefinition startJob(String jobName, @Nullable Map<String, Object> params) throws JobAlreadyStartedException, SchedulerException;

	/**
	 * Enregistre le job spécifié pour exécution dès que possible (= immédiatement si le job ne tourne pas déjà ou dès la fin de l'exécution du job en cours).
	 *
	 * @param jobName le nom du job à démarrer
	 * @param params  les paramètres du job
	 * @return la définition du job
	 * @throws SchedulerException         en cas d'erreur de scheduling Quartz
	 */
	JobDefinition queueJob(@NotNull String jobName, @Nullable Map<String, Object> params) throws SchedulerException;

	/**
	 * @return la map des jobs enregistrés
	 */
	Map<String, JobDefinition> getJobs();

	/**
	 * @param name nom du job recherché
	 * @return job recherché par son nom
	 */
	JobDefinition getJob(String name);

	/**
	 * Retourne la liste des jobs triés
	 * @return les jobs triés
	 */
	List<JobDefinition> getSortedJobs();

	/**
	 * Arrête l'exécution d'un job et ne retourne que lorsque le job est vraiment arrêté.
	 *
	 * @param name le nom du job à arrêter
	 * @param timeout (optionel) si fourni, ne rend la main qu'après que le job est vraiment arrêté ou que le timeout soit écoulé ; si absent, retour immédiat
	 * @throws SchedulerException en cas d'erreur de scheduling Quartz
	 */
	void stopJob(String name, @Nullable Duration timeout) throws SchedulerException;

	/**
	 * Demande à tous les jobs en cours de s'arrêter
	 * @return <code>true</code> si tous les jobs ont pu être arrêtés, <code>false</code> si certains n'ont pas voulu/pu s'arrêter
	 */
	boolean stopAllRunningJobs();
}

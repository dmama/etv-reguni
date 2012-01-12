package ch.vd.moscow.database;

import ch.vd.moscow.job.JobStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.moscow.data.Environment;

/**
 * @author msi
 */
public interface DatabaseService {

	void clearDb();

    /**
     * Import a calls log for a particular environment
     *
     * @param environment the target environment
     * @param filename    the file log
     * @param status
     */
    void importLog(Environment environment, String filename, JobStatus status);
}

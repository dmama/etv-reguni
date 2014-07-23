package ch.vd.moscow.database;

import org.jetbrains.annotations.Nullable;

import ch.vd.moscow.data.Environment;
import ch.vd.moscow.job.JobStatus;

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
    void importLog(Environment environment, String filename, @Nullable JobStatus status);
}

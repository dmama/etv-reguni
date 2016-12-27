package ch.vd.uniregctb.migreg;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.type.TypeMigRegError;

public interface MigrationErrorDAO extends GenericDAO<MigrationError, Long> {

	MigrationError getErrorForContribuable(long numeroCtb);

	boolean existsForContribuable(long numeroCtb);

	void removeForContribuable(long numeroCtb);

	List<Long> getAllNoCtb();

	List<Long> getAllNoCtbForTypeError(TypeMigRegError type);

	List<Long> getAllNoCtbForTypeErrorNeq(TypeMigRegError type);

	List<MigrationError> getMigregErrorsInCtbRange(int ctbStart, int ctbEnd);

}

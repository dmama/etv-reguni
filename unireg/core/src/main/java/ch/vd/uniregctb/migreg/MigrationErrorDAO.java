package ch.vd.uniregctb.migreg;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.type.TypeMigRegError;

public interface MigrationErrorDAO extends GenericDAO<MigrationError, Long> {

	public MigrationError getErrorForContribuable(long numeroCtb);

	public boolean existsForContribuable(long numeroCtb);

	public void removeForContribuable(long numeroCtb);

	public List<Long> getAllNoCtb();

	public List<Long> getAllNoCtbForTypeError(TypeMigRegError type);

	public List<Long> getAllNoCtbForTypeErrorNeq(TypeMigRegError type);

	public List<MigrationError> getMigregErrorsInCtbRange(int ctbStart, int ctbEnd);

}

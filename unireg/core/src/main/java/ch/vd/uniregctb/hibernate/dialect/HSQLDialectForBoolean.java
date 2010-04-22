package ch.vd.uniregctb.hibernate.dialect;

import java.sql.Types;


public class HSQLDialectForBoolean extends org.hibernate.dialect.HSQLDialect {

    public HSQLDialectForBoolean() {
        super();
        registerColumnType(Types.BIT, "tinyint");

        /*
        // Assert that the new type is registered correctly.
        if (!"boolean".equals(getTypeName(Types.BIT))) {
            throw new IllegalStateException("Failed to register HSQLDialect "
                    + "column type for Types.BIT to \"boolean\".");
        }
        */
	        
	    }
}

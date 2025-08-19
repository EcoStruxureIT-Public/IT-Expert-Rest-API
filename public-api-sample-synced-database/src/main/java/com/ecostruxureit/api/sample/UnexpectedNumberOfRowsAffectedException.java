/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;
/**
 * Is thrown by repository methods, if the number of rows that were actually updated by a given SQL statement, does not match the number of
 * rows we expected to be updated. This also means that the transaction making the change will be rolled back (because of Spring's
 * {@link org.springframework.transaction.annotation.Transactional} annotation used in the program).
 */
class UnexpectedNumberOfRowsAffectedException extends RuntimeException {

    UnexpectedNumberOfRowsAffectedException(int expectedNumberOfRowsAffected, int actualNumberOfRowsAffected) {
        super("Expected " + expectedNumberOfRowsAffected + " rows to have been affected, but "
                + actualNumberOfRowsAffected + " were");
    }
}

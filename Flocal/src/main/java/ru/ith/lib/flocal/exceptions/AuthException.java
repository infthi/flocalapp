package ru.ith.lib.flocal.exceptions;

import ru.ith.lib.flocal.FLException;

public class AuthException extends FLException {

	public AuthException(String message, String details) {
		super(message, details);
	}

}

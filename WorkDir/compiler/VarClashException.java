package compiler;

/**
 * A VarClashException is thrown when two variables in the same scope are found
 * to have the same name.
 * 
 * @author Brad Richards
 */

@SuppressWarnings("serial")
public class VarClashException extends java.lang.Exception {
	public VarClashException(String msg) {
		super(msg); // This was the default, I changed it --Andrew
		System.out.println(msg);
	}
}

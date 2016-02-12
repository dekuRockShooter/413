import java.util.*;
import java.util.function.BiPredicate;


/**
 * Evaluates simple mathematical expressions.
 */
public class Evaluator {
    /*
     * The classes that implement BiPredicate determine the conditions
     * required to pop the Operator stack.  There are several factors that
     * determine when to pop the stack, for example whether or not the new
     * operator is left or right associative, or the end of a group (such as
     * closing parenthesis, brackets, etc.).  Each implementing class defines
     * these conditions for a specific family of operators.
     *
     * All concrete classes must implement the method test, which returns true
     * if the operator stack should be popped, and false otherwise.  If an
     * Operator is popped, then two Operands are popped as well, and they are
     * passed to the popped Operator's execute method.
     *
     * BiPredicate concrete classes:
     *  LeftAssociativeEval
     *  RightAssociativeEval
     *  GroupEval
     */

    /**
     * Determines when to pop if an Operator is left associative.
     */
    private class LeftAssociativeEval implements
        BiPredicate<Operator, Operator> {

        /**
         * @param topOpr the operator at the top of the operator stack
         * @param newOpr a left associative operator
         * @return <code>true</code> if newOpr's precedence is less than or
         *         equal to topOpr's.
         *         <code>false</code> otherwise.
         * */
        public boolean test(Operator topOpr, Operator newOpr) {
            return (topOpr.priority() >= newOpr.priority());
        }
    };

    /**
     * Determines when to pop if an Operator is right associative.
     */
    private class RightAssociativeEval implements
        BiPredicate<Operator, Operator> {

        /**
         * @param topOpr the operator at the top of the operator stack
         * @param newOpr a right associative operator
         * @return <code>true</code> if newOpr's precedence is less than
         *         than topOpr's.
         *         <code>false</code> otherwise.
         * */
        public boolean test(Operator topOpr, Operator newOpr) {
            return (topOpr.priority() > newOpr.priority());
        }
    };

    /**
     * Determines when to pop if an Operator is the end of an expression.
     */
    private class GroupEval implements BiPredicate<Operator, Operator> {
        // The Operator that marks the beginning of an expression.
        Operator startOpr = null;

        /**
         * @param topOpr the operator at the top of the operator stack
         * @param newOpr the operator that marks the start of an expression
         * @return <code>true</code> if topOpr is not newOpr.
         *         <code>false</code> otherwise.
         * */
        public boolean test(Operator topOpr, Operator newOpr) {
            if (this.startOpr != newOpr)
                this.startOpr = newOpr;
            return topOpr != this.startOpr;
        }
    };

    private final Stack<Operand> opdStack;
    private final Stack<Operator> oprStack;
    /*
     * A stack of Operators that denote the start of an expression.  Every time
     * such an operand is encountered, it is pushed to this stack.  This way,
     * we keep track of the most recent operand that started an expression so
     * that * we can make sure that it and the next operand that ends an
     * expression match (if they don't then something is unbalanced).  This
     * stack also allows a simple way to change the priority of an operand
     * that starts an expression.
     * */
    private final Stack<Operator> groupStartStack;
    private final BiPredicate<Operator, Operator> rightAssociativeEval;
    private final BiPredicate<Operator, Operator> leftAssociativeEval;
    private final BiPredicate<Operator, Operator> groupEval;
    /*
     * A map that associates Operators that terminate an expression with those
     * that start one.  For example, ) -> (, ] -> [, etc.
     * */
    private final HashMap<Operator, Operator> groupOprMap;

    public Evaluator() {
        opdStack = new Stack<Operand>();
        oprStack = new Stack<Operator>();
        groupStartStack = new Stack<Operator>();
        rightAssociativeEval = new RightAssociativeEval();
        leftAssociativeEval = new LeftAssociativeEval();
        groupEval = new GroupEval();
        groupOprMap = new HashMap<>();
        groupOprMap.put(Operator.operatorMap.get(")"),
                        Operator.operatorMap.get("("));
        groupOprMap.put(Operator.operatorMap.get("!"),
                        Operator.operatorMap.get("#"));
    }

    /**
     * Computes a mathematical expression.
     *
     * @param expr the mathematical expression to evaluate, using infix notation
     *             consisting of integers and the operators +, -, *, /, and ^.
     * @return the integer that the expression evaluates to.
     */
    public int eval(String expr) {
        // Mark the bottom of the stack.
        oprStack.push(Operator.operatorMap.get("#"));
        groupStartStack.push(Operator.operatorMap.get("#"));
        expr = expr + "!";
        String delimiters = "+-*/^#!() ";
        String tok;
        Operator newOpr = null;
        Operator oldOpr = null;
        // the 3rd arg is true to indicate to use the delimiters as token
        // but we'll filter out spaces
        StringTokenizer st = new StringTokenizer(expr,delimiters,true);
        while (st.hasMoreTokens()) {
            if ( (tok = st.nextToken()).equals(" ")) 
                continue;
            if (Operand.check(tok)) { // check if tok is an operand
                opdStack.push(new Operand(tok));
            }
            else {
                if (!Operator.check(tok)) {
                    System.out.println("*****invalid token******");
                    System.exit(1);
                }
                newOpr = Operator.operatorMap.get(tok);
                // The Operator marks the end of an expression, so evaluate
                // everything until we reach the beginning of the expression.
                if (this.groupOprMap.containsKey(newOpr)) {
                    // The call to get gets the operand that marks the
                    // beginning of the expression.
                    this.popOperator(this.groupEval,
                                     this.groupOprMap.get(newOpr));
                    // Don't forget to remove the start-of-expression operand.
                    oprStack.pop();
                    groupStartStack.pop();
                    continue;
                }
                oldOpr = oprStack.peek();
                if (oldOpr.ASSOCIATIVITY == Operator.Associativity.RIGHT) {
                    this.popOperator(this.rightAssociativeEval, newOpr);
                }
                else if (oldOpr.ASSOCIATIVITY == Operator.Associativity.LEFT) {
                    this.popOperator(this.leftAssociativeEval, newOpr);
                }
                oprStack.push(newOpr);
            }
        }
        return opdStack.pop().getValue();
    }

    /**
     * Executes an <code>Operator</code> until a condition is met.
     *
     * An <code>Operator</code> is popped from the stack and is used to
     * evaluate <code>Operand</code>s.  This continues until the given
     * predicate evaluates to <code>false</code>.
     *
     * @param p a predicate used to determine when to remove and execute an
     *          <code>Operator</code>
     */
    private void popOperator(BiPredicate<Operator, Operator> p,
                             Operator newOpr) {
        Operand lhs = null;
        Operand rhs = null;
        Operand res = null;
        Operator oldOpr = oprStack.peek();
        // newOpr marks the start of the most recent expression.
        if (newOpr.isGroupStart()) {
            // Change the priority of the Operand that starts an expression so
            // that it will be pushed on top of all other operators.
            this.groupStartStack.push(newOpr);
            newOpr.setOutPriority();
        }
        else
            // Change the priority of the most recent Operand that starts an
            // expression so that other operators can be pushed on top of it.
            this.groupStartStack.peek().setInPriority();
        while (p.test(oldOpr, newOpr)) {
            rhs = opdStack.pop();
            lhs = opdStack.pop();
            res = oldOpr.execute(lhs, rhs);
            opdStack.push(res);
            oprStack.pop();
            oldOpr = oprStack.peek();
        }
    }
}


/**
 * A binary mathematical operator.
 */
abstract class Operator {
    /**
     * The associativity of an <code>Operator</code>
     */
    static enum Associativity {LEFT, RIGHT};
    private static final String validOperators = "+-/*^()";
    private final String operator;

    /**
     * The associativity of this <code>Operator</code>
     */
    final Associativity ASSOCIATIVITY;

    /**
     * A map from an operator's string to an <code>Operator</code> object.
     * Use this map to access Operators.
     */
    static final HashMap<String, Operator> operatorMap;
    static {
        operatorMap = new HashMap<String, Operator>();
        operatorMap.put("+", new AdditionOperator());
        operatorMap.put("-", new SubtractionOperator());
        operatorMap.put("*", new MultiplicationOperator());
        operatorMap.put("/", new DivisionOperator());
        operatorMap.put("^", new ExponentiationOperator());
        operatorMap.put("#", new BegOperator());
        operatorMap.put("!", new EndOperator());
        operatorMap.put("(", new OpenParenthesis());
        operatorMap.put(")", new CloseParenthesis());
    }

    /**
     * Creates a new arithmetic operator.
     *
     * @param operator a string that denotes the operator
     * @param associativity the associativity of the operator
     * @see Operator#Associativity
     * */
    Operator(String operator, Associativity associativity) {
        this.operator = operator;
        this.ASSOCIATIVITY = associativity;
    }

    /**
     * @return the priority of this <code>Operator</code>
     * */
    abstract int priority();

    /**
     * Changes the priority of this <code>Operator</code> when it is in the
     * Operator stack.
     * */
    void setInPriority() {}

    /**
     * Changes the priority of this <code>Operator</code> when it is not in
     * the Operator stack.
     * */
    void setOutPriority() {}

    /**
     * @return <code>true</code> if this <code>Operand</code> denotes the
     *         start of an expression.
     *         <code>false</code> otherwise.
     * */
    boolean isGroupStart() { return false; }

    /**
     * Executes this <code>Operator</code> on the given <code>Operands</code>
     *
     * @return the <code>Operand</code> that is the result of the execution
     * */
    abstract Operand execute(Operand lhs, Operand rhs);

    /**
     * Checks whether a string is a valid operator.
     *
     * @param operator the string to validate.
     * @return <code>true</code> if the given string is a valid arithmetic
     *         operator.
     *         <code>false</code> otherwise.
     * */
    static boolean check(String operator) {
        return Operator.operatorMap.containsKey(operator);
    }
}


class AdditionOperator extends Operator {
    final int priority;

    AdditionOperator() {
        super("+", Operator.Associativity.LEFT);
        this.priority = 2;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() + rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }
}


class SubtractionOperator extends Operator {
    final int priority;

    SubtractionOperator() {
        super("-", Operator.Associativity.LEFT);
        this.priority = 2;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() - rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }
}


class MultiplicationOperator extends Operator {
    final int priority;

    MultiplicationOperator() {
        super("*", Operator.Associativity.LEFT);
        this.priority = 3;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() * rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }
}


class DivisionOperator extends Operator {
    final int priority;

    DivisionOperator() {
        super("/", Operator.Associativity.LEFT);
        this.priority = 3;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() / rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }
}


class ExponentiationOperator extends Operator {
    final int priority;

    ExponentiationOperator() {
        super("^", Operator.Associativity.RIGHT);
        this.priority = 4;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand((int)(Math.pow(lhs.getValue(), rhs.getValue())));
    }

    @Override
    int priority() { return this.priority; }
}


/**
 * Represents the beginning of an expression.
 */
class BegOperator extends Operator {
    final int priority;

    BegOperator() {
        super("#", Operator.Associativity.LEFT);
        this.priority = 0;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(0);
    }

    @Override
    int priority() { return this.priority; }
}


/**
 * Represents the end of an expression.
 */
class EndOperator extends Operator {
    final int priority;

    EndOperator() {
        super("!", Operator.Associativity.LEFT);
        this.priority = 8;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(0);
    }

    @Override
    int priority() { return this.priority; }
}


/**
 * Represents the beginning of a sub-expression.
 */
class OpenParenthesis extends Operator {
    int priority;

    OpenParenthesis() {
        super("(", Operator.Associativity.RIGHT);
        this.priority = 10;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() + rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }

    @Override
    void setInPriority() {
        this.priority = 0;
    }

    @Override
    void setOutPriority() {
        this.priority = 10;
    }

    @Override
    boolean isGroupStart() { return true; }
}


/**
 * Represents the end of a sub-expression.
 */
class CloseParenthesis extends Operator {
    final int priority;

    CloseParenthesis() {
        super(")", Operator.Associativity.LEFT);
        this.priority = 10;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() + rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }
}


/**
 * An integer.
 * */
class Operand {
    private int value;

    /**
     * Creates an <code>Operand</code> from a <code>String</code>.
     *
     * @param operand a string representation of an integer
     * */
    Operand(String operand) {this.value = Integer.parseInt(operand);}

    /**
     * Creates an <code>Operand</code> from an <code>int</code>.
     *
     * @param operand the integer to be this Operand's value
     * */
    Operand(int operand) {this.value = operand;}

    /**
     * Returns the value of this operand.
     *
     * @return the integer that this operand represents.
     * */
    int getValue() { return this.value; }

    /**
     * Checks whether a string is a valid operand.  A valid operand is any
     * integer.
     *
     * @param operand the string to validate.
     * @return <code>true</code> if the given string represents an integer.
     * @return <code>false</code> otherwise.
     * */
    static boolean check(String operand) {
        try {
            Integer.parseInt(operand);
        }
        catch (NumberFormatException err) {
            return false;
        }
        return true;
    }
}

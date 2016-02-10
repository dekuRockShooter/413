//package evaluator;

import java.util.*;
import java.util.function.BiPredicate;


public class Evaluator {
    /**
     * Determines the conditions required to pop the Operator stack.  The
     * popped Operator is used to evaluate the top two Operands from the 
     * Operand stack.
     */
    private class LeftAssociativeEval implements
        BiPredicate<Operator, Operator> {

        public boolean test(Operator topOpr, Operator newOpr) {
            return (topOpr.priority() >= newOpr.priority());
        }
    };

    private class RightAssociativeEval implements
        BiPredicate<Operator, Operator> {

        public boolean test(Operator topOpr, Operator newOpr) {
            return (topOpr.priority() > newOpr.priority());
        }
    };

    private class AllEval implements BiPredicate<Operator, Operator> {

        public boolean test(Operator topOpr, Operator newOpr) {
            return topOpr.priority() > 0;
        }
    };

    private class GroupEval implements BiPredicate<Operator, Operator> {
        Operator startOpr = null;

        public boolean test(Operator topOpr, Operator newOpr) {
            if (this.startOpr != newOpr)
                this.startOpr = newOpr;
            return topOpr != this.startOpr;
        }
    };

    private final Stack<Operand> opdStack;
    private final Stack<Operator> oprStack;
    private final Stack<Operator> groupStartStack;
    private final BiPredicate<Operator, Operator> rightAssociativeEval;
    private final BiPredicate<Operator, Operator> leftAssociativeEval;
    private final BiPredicate<Operator, Operator> allEval;
    private final BiPredicate<Operator, Operator> groupEval;
    private final HashMap<Operator, Operator> groupOprMap;

    public Evaluator() {
        opdStack = new Stack<Operand>();
        oprStack = new Stack<Operator>();
        groupStartStack = new Stack<Operator>();
        rightAssociativeEval = new RightAssociativeEval();
        leftAssociativeEval = new LeftAssociativeEval();
        allEval = new AllEval();
        groupEval = new GroupEval();
        groupOprMap = new HashMap<>();
        groupOprMap.put(Operator.operatorMap.get(")"), Operator.operatorMap.get("("));
        groupOprMap.put(Operator.operatorMap.get("!"), Operator.operatorMap.get("#"));
    }

    /**
     * Computes a mathematical expression.
     *
     * @param expr the mathematical expression to evaluate, using infix notation
     *             and consisting of the operators +, -, *, /, and ^, and
     *             integers.
     * @return the integer that the expression evaluates to.
     */
    public int eval(String expr) {
        // Mark the bottom of the stack.
        oprStack.push(Operator.operatorMap.get("#"));
        groupStartStack.push(Operator.operatorMap.get("#"));
        String delimiters = "+-*/^#!() ";
        String tok;
        Operator newOpr = null;
        Operator oldOpr = null;
        // the 3rd arg is true to indicate to use the delimiters as token
        // but we'll filter out spaces
        StringTokenizer st = new StringTokenizer(expr,delimiters,true);
        // Convert infix to postfix.
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
                if (this.groupOprMap.containsKey(newOpr)) {
                    this.popOperator(this.groupEval, this.groupOprMap.get(newOpr));
                    oprStack.pop(); // Pop the open parenthesis.
                    groupStartStack.pop(); // Pop the open parenthesis.
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
        // Evaluate the entire stack.
        this.popOperator(this.allEval, newOpr);
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
        // The open parenthesis acts as the bottom of the stack.  It is
        // essentially the beginning of a new expression.
        if (newOpr.isGroupStart()) {
            newOpr.setOutPriority(); // priority when out
            this.groupStartStack.push(newOpr);
        }
        else
            this.groupStartStack.peek().setInPriority(); 
        // while the predicate is true, execute the Operator on the top
        // two Operands.
        // for r in open, close
        while (p.test(oldOpr, newOpr)) {
            rhs = opdStack.pop();
            lhs = opdStack.pop();
            res = oldOpr.execute(lhs, rhs);
            opdStack.push(res);
            oprStack.pop();
            oldOpr = oprStack.peek();
        }
        this.groupStartStack.peek().setOutPriority(); 
    }
}


/**
 * A basic mathematical binary operator.
 */
abstract class Operator {
    /**
     * The associativity of an <code>Operator</code>
     */
    public static enum Associativity {LEFT, RIGHT};
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
        operatorMap.put("#", new EndOperator());
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

    void setInPriority() {}
    void setOutPriority() {}

    boolean isTerminal() { return false; }
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
        return Operator.validOperators.contains(operator);
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


class EndOperator extends Operator {
    final int priority;

    EndOperator() {
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


class BegOperator extends Operator {
    final int priority;

    BegOperator() {
        super("!", Operator.Associativity.LEFT);
        this.priority = 8;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(0);
    }

    @Override
    int priority() { return this.priority; }

    @Override
    boolean isTerminal() { return true; }
}


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
 * An operand in a mathematical expression.
 * <p>
 * The operand can be any integer that can be operated on using the operators
 * +, -, *, /, and ^.
 * */
class Operand {
    private int value;

    /**
     * Creates an operand from a <code>String</code>.
     *
     * @param operand a string representation of an integer
     * */
    Operand(String operand) { this.value = Integer.parseInt(operand); }

    /**
     * Creates an operand from an <code>int</code>.
     *
     * @param operand the integer that is this Operand's value
     * */
    Operand(int operand) { this.value = operand; }

    /**
     * Returns the value of this operand.
     *
     * @return the integer that this operand represents.
     * */
    int getValue() { return this.value; }

    /**
     * Checks whether a string is a valid operand.  A valid operand is any
     * integer.  Thus, the string is valid if it represents an integer.
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

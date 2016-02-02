//package evaluator;

import java.util.*;
import java.util.function.BiPredicate;


interface Strategy {
    boolean execute(Operator opr);
}


public class Evaluator {
    private Stack<Operand> opdStack;
    private Stack<Operator> oprStack;

    public Evaluator() {
        opdStack = new Stack<Operand>();
        oprStack = new Stack<Operator>();
    }

    /**
     * Computes an arithmetic expression.
     *
     * @param expr the arithmetic expression to evaluate, using infix notation
     *             and consisting of arithmetic operators and integers.
     * @return the integer that the expression evaluates to.
     */
    public int eval(String expr) {
        String tok;
        // init stack - necessary with operator priority schema;
        // the priority of any operator in the operator stack other then
        // the usual operators - "+-*/" - should be less than the priority
        // of the usual operators
        // When is good time to add “!” operator?
        oprStack.push(Operator.operatorMap.get("#"));
        String delimiters = "+-*/^#!() ";
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
                Operator newOpr = Operator.operatorMap.get(tok);
                if (newOpr == Operator.operatorMap.get(")")) {
                    while (oprStack.peek() != Operator.operatorMap.get("(")) {
                        Operator oldOpr = oprStack.pop();
                        Operand rhs = (Operand)opdStack.pop();
                        Operand lhs = (Operand)opdStack.pop();
                        opdStack.push(oldOpr.execute(lhs, rhs));
                    }
                    oprStack.pop(); // pop the open parenthesis.
                    continue;
                }
                Operator oldOpr = oprStack.peek();
                if (oldOpr == Operator.operatorMap.get("^")){
                    evalHelper2((Operator opr) -> {
                         return ((oprStack.peek().priority() > newOpr.priority()) &&
                                 (opr != Operator.operatorMap.get("(")));
                    });
                }
                else {
                    evalHelper2((Operator opr) -> {
                         return ((oprStack.peek().priority() >= newOpr.priority()) &&
                                 (opr != Operator.operatorMap.get("(")));
                    });
                }
                oprStack.push(newOpr);
            }
        }
        return this.evaluate(oprStack, opdStack);
    }

    //private void evalHelper2(BiPredicate<Operator, Operator> predicate) {
    private void evalHelper2(Strategy s) {
        Operator oldOpr = oprStack.peek();
        while (s.execute(oldOpr)) {
            oldOpr = ((Operator)oprStack.pop());
            Operand op2 = (Operand)opdStack.pop();
            Operand op1 = (Operand)opdStack.pop();
            opdStack.push(oldOpr.execute(op1,op2));
        }
    }
    private void evalHelper(BiPredicate<Operator, Operator> predicate) {
        //Operator oldOpr = oprStack.peek();
        //Operator newOpr = Operator.operatorMap.get(tok);
        //while (predicate()) {
            //oldOpr = ((Operator)oprStack.pop());
            //Operand op2 = (Operand)opdStack.pop();
            //Operand op1 = (Operand)opdStack.pop();
            //opdStack.push(oldOpr.execute(op1,op2));
        //}
    }

    private int evaluate(Stack<Operator> oprStack, Stack<Operand> opdStack) {
        Operand lhs = null;
        Operand rhs = null;
        Operand res = null;
        Operator opr = oprStack.pop();
        while (opr != Operator.operatorMap.get("#")) {
            rhs = opdStack.pop();
            lhs = opdStack.pop();
            res = opr.execute(lhs, rhs);
            opdStack.push(res);
            opr = oprStack.pop();
        }
        return opdStack.pop().getValue();
    }
}


abstract class Operator {
    private static final String validOperators = "+-/*^()";
    private final String operator;
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
     * @param operator a string that denotes the operator (one of +, -, *, /)
     * */
    Operator(String operator) {
        this.operator = operator;
    }

    // TODO: abstract
    abstract int priority();

    // TODO: abstract
    abstract Operand execute(Operand lhs, Operand rhs);

    /**
     * Checks whether a string is a valid operator.  A valid operator is any
     * arithmetic operator (+, -, *, /).
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
        super("+");
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
        super("-");
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
        super("*");
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
        super("/");
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
        super("^");
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
        super("#");
        this.priority = 0;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(0);
    }

    @Override
    int priority() { return this.priority; }
}


class OpenParenthesis extends Operator {
    final int priority;

    OpenParenthesis() {
        super("(");
        this.priority = 5;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() + rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }
}


class CloseParenthesis extends Operator {
    final int priority;

    CloseParenthesis() {
        super(")");
        this.priority = 5;
    }

    @Override
    Operand execute(Operand lhs, Operand rhs) {
        return new Operand(lhs.getValue() + rhs.getValue());
    }

    @Override
    int priority() { return this.priority; }
}


/**
 * An operand in an arithmetic expression.
 * <p>
 * The operand can be any integer that can be operated on using the arithmetic
 * operators (+, -, *, /).
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

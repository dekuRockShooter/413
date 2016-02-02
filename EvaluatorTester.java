public class EvaluatorTester {

    public static void main(String[] args) {
        Evaluator anEvaluator = new Evaluator();
        for (String arg : args) {
            System.out.println(arg + " = " + anEvaluator.eval(arg));
        }
    }
}

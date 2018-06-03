import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

// An assignment on decision trees, using the "Adult" dataset from
// the UCI Machine Learning Repository.  The dataset predicts
// whether someone makes over $50K a year from their census data.
//
// Input data is a comma-separated values (CSV) file where the
// target classification is given a label of "Target."
// The other headers in the file are the feature names.
//
// Features are assumed to be strings, with comparison for equality
// against one of the values as a decision, unless the value can
// be parsed as a double, in which case the decisions are < comparisons
// against the values seen in the data.

public class DecisionTree {

  public Feature feature;   // if true, follow the yes branch
  public boolean decision;  // for leaves
  public DecisionTree yesBranch;
  public DecisionTree noBranch;


  public static double CHI_THRESH = 3.84;  // chi-square test critical value
  public static double EPSILON = 0.00000001; // for determining whether vals roughly equal
  public static boolean PRUNE = true;  // prune with chi-square test or not

  public static void main(String[] args) throws FileNotFoundException {
    //    Scanner scanner = new Scanner(System.in);
    Scanner scanner = new Scanner(new File("test2.txt"));


    // Keep header line around for interpreting decision trees
    String header = scanner.nextLine();
    Feature.featureNames = header.split(",");
    System.err.println("Reading training examples...");

    ArrayList<Example> trainExamples = readExamples(scanner);
    // We'll assume a delimiter of "---" separates train and test as before

    DecisionTree tree = new DecisionTree(trainExamples);

    System.out.println(tree);
    System.out.println("Training data results: ");
    System.out.println(tree.classify(trainExamples));
    System.err.println("Reading test examples...");

    ArrayList<Example> testExamples = readExamples(scanner);
    Results results = tree.classify(testExamples);
    System.out.println("Test data results: ");
    System.out.print(results);
  }

  public static ArrayList<Example> readExamples(Scanner scanner) {
    ArrayList<Example> examples = new ArrayList<Example>();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.startsWith("---")) {
        break;
      }
      // Skip missing data lines
      if (!line.contains("?")) {
        Example newExample = new Example(line);
        examples.add(newExample);
      }
    }
    return examples;
  }

  public static class Example {
    // Not all features will use both arrays.  The Feature.isNumerical static
    // array will determine whether the numericals array can be used.  If not,
    // the strings array will be used.  The indices correspond to the columns
    // of the input, and thus the different features.  "target" is special
    // as it gives the desired classification of the example.
    public String[] strings;     // Use only if isNumerical[i] is false
    public double[] numericals;  // Use only if isNumerical[i] is true
    boolean target;

    // Construct an example from a CSV input line
    public Example(String dataline) {
      // Assume a basic CSV with no double-quotes to handle real commas
      strings = dataline.split(",");
      // We'll maintain a separate array with everything that we can
      // put into numerical form, in numerical form.
      // No real need to distinguish doubles from ints.
      numericals = new double[strings.length];
      if (Feature.isNumerical == null) {
        // First data line; we're determining types
        Feature.isNumerical = new boolean[strings.length];
        for (int i = 0; i < strings.length; i++) {
          if (Feature.featureNames[i].equals("Target")) {
            target = strings[i].equals("1");
          } else {
            try {
              numericals[i] = Double.parseDouble(strings[i]);
              Feature.isNumerical[i] = true;
            } catch (NumberFormatException e) {
              Feature.isNumerical[i] = false;
              // string stays where it is, in strings
            }
          }
        }
      } else {
        for (int i = 0; i < strings.length; i++) {
          if (i >= Feature.isNumerical.length) {
            System.err.println("Too long line: " + dataline);
          } else if (Feature.featureNames[i].equals("Target")) {
            target = strings[i].equals("1");
          } else if (Feature.isNumerical[i]) {
            try {
              numericals[i] = Double.parseDouble(strings[i]);
            } catch (NumberFormatException e) {
              Feature.isNumerical[i] = false;
              // string stays where it is
            }
          }
        }
      }
    }

    // Possibly of help in debugging:  a way to print examples
    public String toString() {
      String out = "";
      for (int i = 0; i < Feature.featureNames.length; i++) {
        out += Feature.featureNames[i] + "=" + strings[i] + ";";
      }
      return out;
    }
  }

  public static class Feature {
    // Which feature are we talking about?  Can index into Feature.featureNames
    // to get name of the feature, or into strings and numericals arrays of example
    // to get feature value
    public int featureNum;
    // WLOG assume numerical features are "less than"
    // and String features are "equal to"
    public String svalue;  // the string value to compare a string feature against
    public double dvalue;  // the numerical threshold to compare a numerical feature against
    public static String[] featureNames;  // extracted from the header
    public static boolean[] isNumerical = null;  // need to read a line to see the size

    public Feature(int featureNum, String value) {
      this.featureNum = featureNum;
      this.svalue = value;
    }

    public Feature(int featureNum, double value) {
      this.featureNum = featureNum;
      this.dvalue = value;
    }

    public Feature(Feature toCopy) {
      this.featureNum = toCopy.featureNum;
      this.svalue = toCopy.svalue;
      this.dvalue = toCopy.dvalue;
    }

    // Ask whether the answer is "yes" or "no" to the question implied by this feature
    // when applied to a particular example
    public boolean apply(Example e) {
      if (Feature.isNumerical[featureNum]) {
        return (e.numericals[featureNum] < dvalue);
      } else {
        return (e.strings[featureNum].equals(svalue));
      }
    }

    // It's suggested that when you generate a collection of potential features, you
    // use a HashSet to avoid duplication of features.  The equality and hashCode operators
    // that follow can help you with this.
    public boolean equals(Object o) {
      if (!(o instanceof Feature)) {
        return false;
      }
      Feature otherFeature = (Feature) o;
      if (featureNum != otherFeature.featureNum) {
        return false;
      } else if (Feature.isNumerical[featureNum]) {
        if (Math.abs(dvalue - otherFeature.dvalue) < EPSILON) {
          return true;
        }
        return false;
      } else {
        if (svalue.equals(otherFeature.svalue)) {
          return true;
        }
        return false;
      }
    }

    public int hashCode() {
      return (featureNum + (svalue == null ? 0 : svalue.hashCode()) + (int) (dvalue * 10000));
    }

    // Print feature's check; called when printing decision trees
    public String toString() {
      if (Feature.isNumerical[featureNum]) {
        return Feature.featureNames[featureNum] + " < " + dvalue;
      } else {
        return Feature.featureNames[featureNum] + " = " + svalue;
      }
    }

  }

  // This constructor should create the whole decision tree recursively.
  DecisionTree(ArrayList<Example> examples) {
    ArrayList<Example> yesExamples = new ArrayList<>();
    ArrayList<Example> noExamples = new ArrayList<>();
    HashSet<Feature> usedFeatures = new HashSet<>();

    Feature bestFeature = findBestFeature(examples, new HashSet<Feature>());
    usedFeatures.add(bestFeature);
    feature = bestFeature;
    //check all examples and sort them into yes or no lists
    for (Example e : examples) {
      if (bestFeature.apply(e)) {
        yesExamples.add(e);
      } else {
        noExamples.add(e);
      }
    }

    yesBranch = buildTree(yesExamples, cloneFeatureHashSet(usedFeatures));
    noBranch = buildTree(noExamples, cloneFeatureHashSet(usedFeatures));


  }

  DecisionTree( boolean finalDecision){
    decision = finalDecision;
  }

  //maybe make this a void function?
  private DecisionTree buildTree(ArrayList<Example> examples, HashSet<Feature> usedFeatures) {
    //check for return condition: total agreement between all examples. If hit, this is a leaf
    //and return begin returning up recursive tree.
    boolean agreementCheck = true;
    if (examples.size() == 1) {
      decision = examples.get(0).target;
      return this;
    } else {
      for (int i = 1; i < examples.size(); i++) {
        if (examples.get(i - 1).target != examples.get(i).target) {
          agreementCheck = false;
        }
      }
    }
    if (agreementCheck) {
      decision = examples.get(0).target;
      return new DecisionTree(decision);
    }

    //otherwise, prepare to find best feature to split.
    ArrayList<Example> yesExamples = new ArrayList<>();
    ArrayList<Example> noExamples = new ArrayList<>();

    Feature bestFeature = findBestFeature(examples, usedFeatures);
    usedFeatures.add(bestFeature);
    feature = bestFeature;
    //check all examples and sort them into yes or no lists
    for (Example e : examples) {
      if (bestFeature.apply(e)) {
        yesExamples.add(e);
      } else {
        noExamples.add(e);
      }
    }
    noBranch = buildTree(noExamples, cloneFeatureHashSet(usedFeatures));
    yesBranch = buildTree(yesExamples, cloneFeatureHashSet(usedFeatures));

    //todo FIGURE THIS OUT.
    return null;
  }

  private Feature findBestFeature(ArrayList<Example> examples, HashSet<Feature> usedFeatures){
    Feature bestFeature = new Feature(0, 0);
    HashSet<Feature> internallyUsedFeatures = new HashSet<>();
    for (Feature f: usedFeatures){
      internallyUsedFeatures.add(f);
    }

    double bestEntropy = 1000000;

    //iterate over all features
    for (int i = 0; i < Feature.featureNames.length; i++) {
      Feature testFeature;
      //ignore target.
      if (!Feature.featureNames[i].equals("Target")) {
        //sample all possible values for that feature present in the examples.
        for (Example e : examples) {
          if (Feature.isNumerical[i]) {
            testFeature = new Feature(i, e.numericals[i]);
          } else {
            testFeature = new Feature(i, e.strings[i]);
          }
          //if that feature hasn't been used before, check the entropy and update best if necessary.
          if (!internallyUsedFeatures.contains(testFeature)) {
            internallyUsedFeatures.add(new Feature(testFeature));
            double testEntropy = entropyCalc(examples, testFeature);
            if (testEntropy < bestEntropy) {
              bestEntropy = testEntropy;
              bestFeature = testFeature;
            }
          }
        }
      }
    }
    return bestFeature;
  }

  private double entropyCalc(ArrayList<Example> examples, Feature testFeature) {
    //total number of examples to be examined.
    double totalNumExamples = examples.size();

    //arrays of examples that are "yes" and "no" in response to the testFeature.
    ArrayList<Example> yesExamples = new ArrayList();
    ArrayList<Example> noExamples = new ArrayList();
    for (Example e : examples) {
      if (testFeature.apply(e)) {
        yesExamples.add(e);
      } else {
        noExamples.add(e);
      }
    }
    //The probability that an example responds as "yes" or "no" to the testFeature.
    double probYesToQuestion = yesExamples.size() / totalNumExamples;
    double probNoToQuestion = noExamples.size() / totalNumExamples;

    //Find the entropy of each set of examples.
    double yesToQuestionEntropy = responseEntropy(yesExamples);
    double noToQuestionEntropy = responseEntropy(noExamples);

    //total entropy is = (probability an example responds yes) * (entropy of the "yes" example set) +
    //                   (probability an example responds no) * (entropy of the "no" example set)
    double totalEntropy = probYesToQuestion * yesToQuestionEntropy + probNoToQuestion * noToQuestionEntropy;

    //return total entropy.
    return totalEntropy;
  }

  //Function used to find the entropy of a given set of examples in relation to the overall target.
  private double responseEntropy(ArrayList<Example> answers){
    //the number of examples that have the positive target.
    double targetPositive = 0;
    //the number of examples that have the negative target.
    double targetNegative = 0;
    //total number of examples in this set.
    double answerSize = (double) answers.size();

    //tally the positive and negative examples.
    for (Example e: answers){
      if (e.target){
        targetPositive++;
      } else {
        targetNegative++;
      }
    }

    //find the probability of a positive or negative example in this set.
    double probPositive = targetPositive / answerSize;
    double probNegative = targetNegative / answerSize;

    //if it's certain that all responses fall into one category, entropy is zero. Return as such.
    if (probPositive == 0 || probNegative == 0){
      return 0;
    }

    //Log2(x) can be expressed as Logk(x) / Logk(2). Using this variable to avoid double computation.
    double base2 = Math.log(2);
    double responseEntropyValue = -(probPositive * (Math.log(probPositive) / base2) +
                                    probNegative * (Math.log(probNegative) / base2));

    return responseEntropyValue;
  }

  private HashSet<Feature> cloneFeatureHashSet(HashSet<Feature> toClone){
    HashSet<Feature> output = new HashSet<>();
    for (Feature f: toClone){
      output.add(new Feature(f));
    }
    return output;
  }


  public static class Results {
    public int true_positive;  // correctly classified "yes"
    public int true_negative;  // correctly classified "no"
    public int false_positive; // incorrectly classified "yes," should be "no"
    public int false_negative; // incorrectly classified "no", should be "yes"

    public Results() {
      true_positive = 0;
      true_negative = 0;
      false_positive = 0;
      false_negative = 0;
    }

    public String toString() {
      String out = "Precision: ";
      out += String.format("%.4f", true_positive / (double) (true_positive + false_positive));
      out += "\nRecall: " + String.format("%.4f", true_positive / (double) (true_positive + false_negative));
      out += "\n";
      out += "Accuracy: ";
      out += String.format("%.4f", (true_positive + true_negative) / (double) (true_positive + true_negative + false_positive + false_negative));
      out += "\n";
      return out;
    }

  }


  public Results classify(ArrayList<Example> examples) {
    Results results = new Results();
    // TODO your code here, classifying each example with the tree and comparing to
    // the truth to populate the results structure
    return results;
  }

  public String toString() {
    return toString(0);
  }

  // Print the decision tree as a set of nested if/else statements.
  // This is a little easier than trying to print with the root at the top.
  public String toString(int depth) {
    String out = "";
    for (int i = 0; i < depth; i++) {
      out += "    ";
    }
    if (feature == null) {
      out += (decision ? "YES" : "NO");
      out += "\n";
      return out;
    }
    out += "if " + feature + "\n";
    out += yesBranch.toString(depth + 1);
    for (int i = 0; i < depth; i++) {
      out += "    ";
    }
    out += "else\n";
    out += noBranch.toString(depth + 1);
    return out;
  }

}
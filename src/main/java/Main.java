import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.io.BNLoaderFromHugin;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, ExceptionHugin {
        BayesianNetwork bn = BNConverterToAMIDST.convertToAmidst(BNLoaderFromHugin.loadFromFile("student.net"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            for (; ; ) {
                String request = br.readLine();
                System.out.println(calculateProbability(bn, request));
            }
        }
    }

    static double calculateProbability(BayesianNetwork bn, String request) {
        String[] variablesDefinitions = request.split(",");
        HashMap<String, Double> varName2Value = new HashMap<>();
        HashMap<String, Boolean> varsNeededForResult = new HashMap<>();
        HashMap<String, HashMap<Double, Double>> var2Value2Probability = new HashMap<>();
        for (String varDef : variablesDefinitions) {
            String[] varDefSplit = varDef.split("=");
            String varName = varDefSplit[0].trim();
            varName2Value.put(varDefSplit[0].trim(), Double.parseDouble(varDefSplit[1].trim()));
            varsNeededForResult.put(varName, true);
        }
        double jointProbability = 1;
        for (Map.Entry<String, Double> varDef : varName2Value.entrySet()) {
            String varName = varDef.getKey();
            if (varsNeededForResult.get(varName)) {
                calculateProbability(bn, varName, varDef.getValue(), varName2Value, varsNeededForResult,
                        var2Value2Probability);
                varsNeededForResult.put(varName, true);
            }
        }
        for (Map.Entry<String, Double> varDef : varName2Value.entrySet()) {
            String varName = varDef.getKey();
            if (varsNeededForResult.get(varName)) {
                jointProbability *= var2Value2Probability.get(varName).get(varDef.getValue());
            }
        }
        return jointProbability;
    }

    private static void calculateProbability(BayesianNetwork bn, String varName, double varValue, HashMap<String,
            Double> varName2Value, HashMap<String, Boolean> varsNeededForResult, HashMap<String, HashMap<Double,
            Double>> var2Value2Probability) {
        varsNeededForResult.put(varName, false);
        if (var2Value2Probability.get(varName) != null &&
                (varName2Value.containsKey(varName) || var2Value2Probability.get(varName).size()
                        == bn.getVariables().getVariableByName(varName).getNumberOfStates())) {
            return;
        }
        ConditionalDistribution distribution =
                bn.getConditionalDistribution(bn.getVariables().getVariableByName(varName));
        HashMapAssignment assignment = new HashMapAssignment();
        assignment.setValue(bn.getVariables().getVariableByName(varName), varValue);
        List<Variable> parents = distribution.getConditioningVariables();
        double requestProbabilities = 1;
        ArrayList<Variable> unNeeededVariables = new ArrayList<>();
        for (int i = 0; i < parents.size(); i++) {
            String currentVarName = parents.get(i).getName();
            if (varsNeededForResult.get(currentVarName) != null && !varsNeededForResult.get(currentVarName)) {
                unNeeededVariables.add(parents.get(i));
                if (varName2Value.get(currentVarName) != null) {
                    assignment.setValue(bn.getVariables().getVariableByName(currentVarName), varName2Value.get(currentVarName));
                }
                continue;
            }
            if (varName2Value.containsKey(currentVarName)) {
                assignment.setValue(bn.getVariables().getVariableByName(currentVarName),
                        varName2Value.get(currentVarName));
                calculateProbability(bn, currentVarName, varName2Value.get(currentVarName), varName2Value,
                        varsNeededForResult, var2Value2Probability);
                requestProbabilities *= var2Value2Probability.get(currentVarName).get(varName2Value.get(currentVarName));
            }
        }
        ArrayList<Variable> tempParents = new ArrayList<>(parents);
        tempParents.removeIf(x -> varName2Value.containsKey(x.getName()) || unNeeededVariables.contains(x));
        parents = tempParents;
        for (Variable var : parents) {
            int numberOfStates = var.getNumberOfStates();
            for (int i = 0; i < numberOfStates; i++) {
                calculateProbability(bn, var.getName(), i, varName2Value, varsNeededForResult, var2Value2Probability);
            }
        }
        double[] result = new double[1];
        calculateCrossProductProbability(parents, bn, requestProbabilities, result, 0, assignment, 1,
                var2Value2Probability, distribution);
        var2Value2Probability.computeIfAbsent(varName, k -> new HashMap<>());
        var2Value2Probability.get(varName).put(varValue, result[0]);
    }

    private static void calculateCrossProductProbability(List<Variable> parents, BayesianNetwork bn,
                                                         double requestProbabilities, double[] result, int depth,
                                                         HashMapAssignment assignment, double parentsFactor,
                                                         HashMap<String, HashMap<Double, Double>> var2Value2Probability, ConditionalDistribution distribution) {
        if (parents.size() == 0) {
            result[0] = distribution.getConditionalProbability(assignment) * requestProbabilities;
            return;
        }
        Variable currentParent = parents.get(depth);
        for (int i = 0; i < currentParent.getNumberOfStates(); i++) {
            assignment.setValue(bn.getVariables().getVariableByName(currentParent.getName()), i);
            double nextFactor = parentsFactor * var2Value2Probability.get(currentParent.getName()).get((double) i);
            if (depth == parents.size() - 1) {
                double currentVarProb = distribution.getConditionalProbability(assignment);
                currentVarProb = currentVarProb * nextFactor * requestProbabilities;
                result[0] += currentVarProb;
            } else {
                calculateCrossProductProbability(parents, bn, requestProbabilities, result, depth + 1, assignment,
                        nextFactor, var2Value2Probability, distribution);
            }
        }
    }
}

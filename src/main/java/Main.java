import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.io.BNLoaderFromHugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException, ExceptionHugin {
        BayesianNetwork bn = BNConverterToAMIDST.convertToAmidst(BNLoaderFromHugin.loadFromFile("asia.net"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            for (; ; ) {
                String request = br.readLine();
                System.out.println(calculateProbability(bn, request));
            }
        }
    }

    public static double calculateProbability(BayesianNetwork bn, String request) {
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
        if (varsNeededForResult.containsKey(varName)) {
            varsNeededForResult.put(varName, false);
        }
        ConditionalDistribution distribution =
                bn.getConditionalDistribution(bn.getVariables().getVariableByName(varName));
        HashMapAssignment assignment = new HashMapAssignment();
        assignment.setValue(bn.getVariables().getVariableByName(varName), varValue);
        List<Variable> parents = distribution.getConditioningVariables();
        ArrayList<Integer> indexesToRemove = new ArrayList<>();
        for (int i = 0; i < parents.size(); i++) {
            String currentVarName = parents.get(i).getName();
            if (varName2Value.containsKey(currentVarName)) {
                indexesToRemove.add(i);
                assignment.setValue(bn.getVariables().getVariableByName(currentVarName),
                        varName2Value.get(currentVarName));
            }
        }
        double requestProbabilities = 1;
        for (int index : indexesToRemove) {
            String currentVarName = parents.get(index).getName();
            calculateProbability(bn, currentVarName, varName2Value.get(currentVarName), varName2Value,
                    varsNeededForResult, var2Value2Probability);
            requestProbabilities *= var2Value2Probability.get(currentVarName).get(varName2Value.get(currentVarName));
        }
        ArrayList<Variable> parentsTemp = new ArrayList<>();
        for (int i = 0; i < parents.size(); i++) {
            if (!indexesToRemove.contains(i)) {
                parentsTemp.add(parents.get(i));
            }
        }
        parents = parentsTemp;
        int counter = 1;
        for (Variable var : parents) {
            int numberOfStates = var.getNumberOfStates();
            counter *= numberOfStates;
            for (int i = 0; i < numberOfStates; i++) {
                calculateProbability(bn, var.getName(), i, varName2Value, varsNeededForResult, var2Value2Probability);
            }
        }
        int parentsSize = parents.size();
        var2Value2Probability.computeIfAbsent(varName, k -> new HashMap<>());
        double result = 0;
        for (int i = 0; i < counter; i++) {
            int[] parentsValues = new int[parentsSize];
            int temp = i;
            int states = 1;
            int currentParent = 0;
            while (temp != 0) {
                int currentStates = parents.get(currentParent).getNumberOfStates();
                int prevState = states;
                states *= currentStates;
                if (temp < states) {
                    parentsValues[currentParent] = temp / prevState;
                    states = prevState;
                    currentParent--;
                    temp -= parentsValues[currentParent] * prevState;
                } else {
                    currentParent++;
                }
            }
            for (int k = 0; k < parentsSize; k++) {
                assignment.setValue(bn.getVariables().getVariableByName(parents.get(k).getName()), parentsValues[k]);
            }
            double currentVarProb = distribution.getConditionalProbability(assignment);
            double parentsFactor = 1;
            for (int t = 0; t < parentsSize; t++) {
                parentsFactor *= var2Value2Probability.get(parents.get(t).getName()).get((double) parentsValues[t]);
            }
            currentVarProb = currentVarProb * parentsFactor * requestProbabilities;
            result += currentVarProb;
        }
        var2Value2Probability.get(varName).put(varValue, result);
    }
}

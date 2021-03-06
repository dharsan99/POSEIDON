package uk.ac.ox.oxfish.maximization.generic;

import uk.ac.ox.oxfish.model.FishState;
import uk.ac.ox.oxfish.model.data.collectors.DataColumn;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

public class ScaledFixedDataLastStepTarget implements DataTarget {

    public static boolean VERBOSE = false;
    private double fixedTarget;
    private String columnName = "";
    private double weight = 1;

    public ScaledFixedDataLastStepTarget() { }

    public double getWeight() { return weight; }

    public void setWeight(double weight) { this.weight = weight; }

    /**
     * computes distance from target (0 best, the higher the number the further away from optimum we are)
     *
     * @param model model after it has been run
     * @return distance from target (0 best, the higher the number the further away from optimum we are)
     */
    @Override
    public double computeError(FishState model) {
        DataColumn simulationOutput = model.getYearlyDataSet().getColumn(columnName);
        if (VERBOSE) {
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("column: " + columnName);
            System.out.println("output: " + simulationOutput.getLatest());
            System.out.println("target: " + fixedTarget);
            System.out.println("error : " + abs((simulationOutput.getLatest() - fixedTarget) / fixedTarget));
        }
        return abs((simulationOutput.getLatest() - fixedTarget) / fixedTarget) * weight;
    }

    public double getFixedTarget() { return fixedTarget; }

    public void setFixedTarget(double fixedTarget) {
        checkArgument(fixedTarget != 0);
        this.fixedTarget = fixedTarget;
    }

    public String getColumnName() { return columnName; }

    public void setColumnName(String columnName) { this.columnName = columnName; }

}

package ij.plugin.filter.SME_PROJECTION_SRC;

/**
 * Created by eltonr on 19/03/16.
 */
public final class SME_ENS_Utils {

    public static double[] convertFloatVecToDoubles(float[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }

    public static float[] convertDoubleVecFloat(double[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        float[] output = new float[input.length];

        for (int i = 0; i < input.length; i++)
        {
            output[i] = (float) input[i];
        }
        return output;
    }

    public static double[][] convertFloatMatrixToDoubles(float[][] input, int dimRows, int dimColoumns)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }

        double[][] output = new double[dimRows][dimColoumns];

        for (int i = 0; i < dimRows; i++) {
            for (int j=0 ; j< dimColoumns; j++) {
                output[i][j] = input[i][j];
            }
        }
        return output;
    }

    public static float[][] convertDoubleMatrixToFloat(double[][] input, int dimRows, int dimColoumns)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }

        float[][] output = new float[dimRows][dimColoumns];

        for (int i = 0; i < dimRows; i++) {
            for (int j=0 ; j< dimColoumns; j++) {
                output[i][j] = (float) input[i][j];
            }
        }

        return output;
    }
}

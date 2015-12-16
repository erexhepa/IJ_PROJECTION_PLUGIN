package ij.plugin.filter;

/**
 * Defines object which register with implementation of the KMean algorithm
 * and is to be notified of significant events during clustering.
 */
public interface MIP_KMeansListener {

    /**
     * A message has been received.
     * 
     * @param message
     */
    public void kmeansMessage(String message);
    
    /**
     * KMeans is complete.
     * 
     * @param clusters the output of clustering.
     * @param executionTime the time in milliseconds taken to cluster.
     */
    public void kmeansComplete(MIP_Cluster[] clusters, long executionTime);
    
    /**
     * An error occurred during KMeans clustering.
     * 
     * @param t
     */
    public void kmeansError(Throwable t);
    
}

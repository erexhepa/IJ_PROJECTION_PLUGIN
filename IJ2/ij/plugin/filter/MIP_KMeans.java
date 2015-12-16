package ij.plugin.filter;

/**
 * K-Means clustering interface.
 */
public interface MIP_KMeans extends Runnable {
    
    /** 
     * Adds a KMeansListener to be notified of significant happenings.
     * 
     * @param l  the listener to be added.
     */
    public void addKMeansListener(MIP_KMeansListener l);
    
    /**
     * Removes a KMeansListener from the listener list.
     * 
     * @param l the listener to be removed.
     */
    public void removeKMeansListener(MIP_KMeansListener l);
    
    /**
     * Get the clusters computed by the algorithm.  This method should
     * not be called until clustering has completed successfully.
     * 
     * @return an array of Cluster objects.
     */
    public MIP_Cluster[] getClusters();
 
}

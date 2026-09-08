package com.gregtechceu.gtceu.api.capability;

public interface IParallelHatch {

    /**
     * @return the current maximum amount of parallelization provided
     */
    int getCurrentParallel();

    /**
     * @return the current minimum amount of parallelization required
     */
    default int getMinimumParallel() {
        return 1;
    }
}

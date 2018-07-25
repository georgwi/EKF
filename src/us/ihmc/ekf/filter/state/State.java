package us.ihmc.ekf.filter.state;

import org.ejml.data.DenseMatrix64F;

public abstract class State
{
   public abstract void setStateVector(DenseMatrix64F newState);

   public abstract void getStateVector(DenseMatrix64F vectorToPack);

   public abstract int getSize();

   /**
    * Predict the state at the next time instance and set this to the prediction.
    */
   public abstract void predict();

   /**
    * Get the matrix that describes the linearized state evolution</br>
    * x(k+1) = A * x(k),<br>
    * where x is the state.
    */
   public abstract void getAMatrix(DenseMatrix64F matrixToPack);

   /**
    * Get the covariance matrix of the state evolution.
    */
   public abstract void getQMatrix(DenseMatrix64F matrixToPack);
}

package us.ihmc.ekf.filter.sensor;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.ekf.filter.Parameters;
import us.ihmc.ekf.filter.state.EmptyState;
import us.ihmc.ekf.filter.state.RobotState;
import us.ihmc.ekf.filter.state.State;

public class JointPositionSensor extends Sensor
{
   private static final int measurementSize = 1;

   private final EmptyState emptyState = new EmptyState();

   private final DenseMatrix64F measurement = new DenseMatrix64F(measurementSize, 1);
   private final DenseMatrix64F R = new DenseMatrix64F(measurementSize, measurementSize);

   private final String jointName;

   public JointPositionSensor(String jointName)
   {
      this.jointName = jointName;
      R.set(0, 0, Parameters.jointPositionSensorCovariance);
   }

   public void setJointPositionMeasurement(double jointPosition)
   {
      measurement.set(0, jointPosition);
   }

   @Override
   public int getMeasurementSize()
   {
      return measurementSize;
   }

   @Override
   public void getMeasurement(DenseMatrix64F vectorToPack)
   {
      vectorToPack.set(measurement);
   }

   @Override
   public void getMeasurementJacobianRobotPart(DenseMatrix64F matrixToPack, RobotState robotState)
   {
      matrixToPack.reshape(measurementSize, robotState.getSize());
      CommonOps.fill(matrixToPack, 0.0);
      matrixToPack.set(0, robotState.findJointPositionIndex(jointName), 1.0);
   }

   @Override
   public void getMeasurementJacobianSensorPart(DenseMatrix64F matrixToPack)
   {
      matrixToPack.reshape(0, 0);
   }

   @Override
   public State getSensorState()
   {
      return emptyState;
   }

   @Override
   public void getRMatrix(DenseMatrix64F matrixToPack)
   {
      matrixToPack.set(R);
   }
}

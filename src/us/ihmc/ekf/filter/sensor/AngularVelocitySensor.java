package us.ihmc.ekf.filter.sensor;

import java.util.List;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.ekf.filter.state.BiasState;
import us.ihmc.ekf.filter.state.RobotState;
import us.ihmc.ekf.filter.state.State;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.robotics.screwTheory.GeometricJacobianCalculator;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class AngularVelocitySensor extends Sensor
{
   private static final int measurementSize = 3;

   private final FrameVector3D measurement;
   private final BiasState biasState;
   private final DoubleProvider covariance;

   private final DenseMatrix64F jacobianMatrix = new DenseMatrix64F(1, 1);
   private final GeometricJacobianCalculator robotJacobian = new GeometricJacobianCalculator();
   private final List<OneDoFJoint> oneDofJoints;

   private final DenseMatrix64F tempRobotState = new DenseMatrix64F(1, 1);

   public AngularVelocitySensor(String sensorName, RigidBody body, ReferenceFrame measurementFrame, boolean estimateBias, YoVariableRegistry registry)
   {
      measurement = new FrameVector3D(measurementFrame);
      robotJacobian.setKinematicChain(ScrewTools.getRootBody(body), body);
      robotJacobian.setJacobianFrame(measurementFrame);
      oneDofJoints = ScrewTools.filterJoints(robotJacobian.getJointsFromBaseToEndEffector(), OneDoFJoint.class);
      covariance = new DoubleParameter(sensorName + "Covariance", registry, 1.0);

      if (estimateBias)
      {
         biasState = new BiasState(sensorName, registry);
      }
      else
      {
         biasState = null;
      }
   }

   @Override
   public State getSensorState()
   {
      return biasState == null ? super.getSensorState() : biasState;
   }

   @Override
   public void getSensorJacobian(DenseMatrix64F jacobianToPack)
   {
      if (biasState == null)
      {
         super.getSensorJacobian(jacobianToPack);
      }
      else
      {
         jacobianToPack.reshape(biasState.getSize(), biasState.getSize());
         CommonOps.setIdentity(jacobianToPack);
      }
   }

   @Override
   public int getMeasurementSize()
   {
      return measurementSize;
   }

   @Override
   public void getRobotJacobianAndResidual(DenseMatrix64F jacobianToPack, DenseMatrix64F residualToPack, RobotState robotState)
   {
      jacobianToPack.reshape(measurementSize, robotState.getSize());
      CommonOps.fill(jacobianToPack, 0.0);

      robotJacobian.computeJacobianMatrix();
      robotJacobian.getJacobianMatrix(jacobianMatrix);

      extractFFromJacobian(jacobianToPack, jacobianMatrix, robotState);

      // Compute the sensor velocity based on the robot state:
      residualToPack.reshape(measurementSize, 1);
      robotState.getStateVector(tempRobotState);
      CommonOps.mult(jacobianToPack, tempRobotState, residualToPack);

      // Compute the residual considering the sensor bias and the current measurement:
      residualToPack.set(0, measurement.getX() - residualToPack.get(0));
      residualToPack.set(1, measurement.getY() - residualToPack.get(1));
      residualToPack.set(2, measurement.getZ() - residualToPack.get(2));

      if (biasState != null)
      {
         residualToPack.set(0, residualToPack.get(0) - biasState.getBias(0));
         residualToPack.set(1, residualToPack.get(1) - biasState.getBias(1));
         residualToPack.set(2, residualToPack.get(2) - biasState.getBias(2));
      }
   }

   private void extractFFromJacobian(DenseMatrix64F FToPack, DenseMatrix64F jacobianMatrix, RobotState robotState)
   {
      int jointOffset = 0;
      if (robotState.isFloating())
      {
         CommonOps.extract(jacobianMatrix, 0, 3, 0, 3, FToPack, 0, robotState.findAngularVelocityIndex());
         CommonOps.extract(jacobianMatrix, 0, 3, 3, 6, FToPack, 0, robotState.findLinearVelocityIndex());
         jointOffset = Twist.SIZE;
      }
      for (int jointIndex = 0; jointIndex < oneDofJoints.size(); jointIndex++)
      {
         int jointVelocityIndex = robotState.findJointVelocityIndex(oneDofJoints.get(jointIndex).getName());
         int jointIndexInJacobian = jointIndex + jointOffset;
         CommonOps.extract(jacobianMatrix, 0, 3, jointIndexInJacobian, jointIndexInJacobian + 1, FToPack, 0, jointVelocityIndex);
      }
   }

   @Override
   public void getRMatrix(DenseMatrix64F matrixToPack)
   {
      matrixToPack.reshape(measurementSize, measurementSize);
      CommonOps.setIdentity(matrixToPack);
      CommonOps.scale(covariance.getValue(), matrixToPack);
   }

   public void setMeasurement(Vector3DReadOnly measurement)
   {
      this.measurement.set(measurement);
   }
}

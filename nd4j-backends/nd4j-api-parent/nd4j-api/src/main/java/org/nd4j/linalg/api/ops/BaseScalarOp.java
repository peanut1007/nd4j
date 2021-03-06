/*-
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */

package org.nd4j.linalg.api.ops;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.graph.intermediate.TGraph;
import org.nd4j.graph.intermediate.TOp;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.tensorflow.framework.NodeDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base scalar operation
 *
 * @author Adam Gibson
 */
@Slf4j
public abstract class BaseScalarOp extends BaseOp implements ScalarOp {
    @Getter
    @Setter
    protected Number num;
    protected IComplexNumber complexNumber;
    public int[] opDimension;


    public BaseScalarOp() {}

    public BaseScalarOp(INDArray x, INDArray y, INDArray z, long n, Number num) {
        super(x, y, z, n);
        this.num = num;
        if (x instanceof IComplexNDArray)
            complexNumber = Nd4j.createComplexNumber(num, 0);

        init(x, y, z, n);
    }

    public BaseScalarOp(INDArray x, Number num) {
        super(x);
        this.num = num;
        if (x instanceof IComplexNDArray)
            complexNumber = Nd4j.createComplexNumber(num, 0);

        init(x, y, z, n);

    }

    public BaseScalarOp(INDArray x, INDArray y, INDArray z, long n, IComplexNumber num) {
        super(x, y, z, n);
        this.complexNumber = num;
        init(x, y, z, n);

    }

    public BaseScalarOp(INDArray x, IComplexNumber num) {
        super(x);
        this.complexNumber = num;
        init(x, y, z, n);

    }


    public BaseScalarOp(SameDiff sameDiff,DifferentialFunction i_v,Number scalar) {
        this(sameDiff,i_v,scalar,false,null);
    }

    public BaseScalarOp(SameDiff sameDiff,DifferentialFunction i_v,Number scalar,boolean inPlace) {
        this(sameDiff,i_v,scalar,inPlace,null);
    }

    public BaseScalarOp(SameDiff sameDiff,
                           DifferentialFunction i_v,
                           Number scalar,
                           boolean inPlace,
                           Object[] extraArgs) {
        super(sameDiff,inPlace,extraArgs);
        this.shape = i_v.getResultShape();
        this.scalarValue = scalar;
        if (i_v != null) {
            this.args = new DifferentialFunction[] {sameDiff.setupFunction(i_v)};
            f().validateFunctionReference(i_v);
            f().validateDifferentialFunctionsameDiff(i_v);
            addAsNewVertexId();
            f().addFunctionEdges(this);
        } else {
            throw new IllegalArgumentException("Input not null variable.");
        }

    }


    public BaseScalarOp(SameDiff sameDiff,
                           DifferentialFunction i_v,
                           Number scalar,
                           Object[] extraArgs) {
        this(sameDiff,i_v,scalar,false,extraArgs);
    }


    @Override
    public List<int[]> calculateOutputShape() {
        List<int[]> ret = new ArrayList<>(1);
        ret.add(arg().getResultShape());
        return ret;
    }

    @Override
    public Type opType() {
        return Type.SCALAR;
    }

    @Override
    public void setScalar(Number scalar) {
        this.num = scalar;
    }

    @Override
    public int broadcastLength() {
        return 1;
    }

    @Override
    public int[] broadcastShape() {
        return new int[] {1, 1};
    }

    @Override
    public Number scalar() {
        return num;
    }

    @Override
    public IComplexNumber complexScalar() {
        return complexNumber;
    }

    @Override
    public int[] getDimension() {
        return opDimension;
    }

    @Override
    public void setDimension(int... dimension) {
        this.opDimension = dimension;
    }


    @Override
    public TOp asIntermediateRepresentation(OnnxProto3.NodeProto node, TGraph graph, Map<String, OnnxProto3.AttributeProto> attributesForNode) {
        return returnIntermediateRepresentation(buildBasicNode(node,graph),graph);

    }

    /**
     * This method returns given TF node as TOp
     *
     * @return
     */
    @Override
    public TOp asIntermediateRepresentation(@NonNull NodeDef node, @NonNull TGraph graph) {
        return returnIntermediateRepresentation(buildBasicNode(node,graph),graph);

    }


    private TOp returnIntermediateRepresentation(TOp tNode,TGraph graph) {

        /**
         * 2 options here. We either have specific dimension, or not.
         * If not - that'll be reduceScalar, if yes - there will be reduceAlongDimension
         */

        log.debug("TOp inputs: {}", tNode.getInputs());
        val shapeIndex = tNode.getInputs().remove(1);

        val variable = graph.getVariableSpace().getVariable(shapeIndex);

        // reduce to scalar
        if (variable.getArray() == null && variable.getShape().length == 2 && variable.getShape()[0] == 1 && variable.getShape()[1] == 1)
            tNode.getOpState().setAxes(new int[]{Integer.MAX_VALUE});// we're going for scalar
        else {
            if (variable.getArray() != null) {
                val axes = variable.getArray().data().asInt();
                tNode.getOpState().setAxes(axes);
            } else
                tNode.getOpState().setAxes(variable.getShape());
        }

        return tNode;
    }

}

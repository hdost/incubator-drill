/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.ref.rops;

import java.util.Collection;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.LogicalPlan;
import org.apache.drill.common.logical.data.Constant;
import org.apache.drill.common.util.FileUtils;
import org.apache.drill.exec.ref.IteratorRegistry;
import org.apache.drill.exec.ref.RecordIterator;
import org.apache.drill.exec.ref.RecordPointer;
import org.apache.drill.exec.ref.ReferenceInterpreter;
import org.apache.drill.exec.ref.RunOutcome;
import org.apache.drill.exec.ref.eval.BasicEvaluatorFactory;
import org.apache.drill.exec.ref.rse.RSERegistry;
import org.apache.drill.exec.ref.values.ScalarValues;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created with IntelliJ IDEA.
 * User: jaltekruse
 * Date: 6/4/13
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConstantROPTest {

    @Test
    public void testConstant(){

        ObjectMapper map = DrillConfig.create().getMapper();
        Constant con;
        try{
            con = map.readValue( FileUtils.getResourceAsString("/constant.json"), Constant.class);
            if (con == null){
                System.out.println("constant is null");
            }
            System.out.println(con);

            ConstantROP rop = new ConstantROP(con);

            rop.setupIterators(new IteratorRegistry());
            RecordIterator iter = rop.getIteratorInternal();
            RecordPointer ptr = iter.getRecordPointer();

            int i = 1;
            while (iter.next() != RecordIterator.NextOutcome.NONE_LEFT){
                System.out.println(ptr);
                org.junit.Assert.assertEquals("Integer value in record " + i + " is incorrect.",
                        ptr.getField(new SchemaPath("c1", ExpressionPosition.UNKNOWN)), new ScalarValues.IntegerScalar(i));
                org.junit.Assert.assertEquals("String value in record " + i + " is incorrect.",
                        ptr.getField(new SchemaPath("c2", ExpressionPosition.UNKNOWN)), new ScalarValues.StringScalar("string " + i));
                i++;
            }
            org.junit.Assert.assertEquals("Incorrect number of records returned by 'constant' record iterator.", 3, i - 1);
        } catch (Exception ex){ ex.printStackTrace(); }
        System.out.println("end test");
    }

    // not sure if we want to keep this as a test and check the results. Now that the internals of the ConstantROP work
    // it might now be worth running the reference intepreter with every build
    @Test
    @Ignore // this plan needs to be updated.
    public void testRefInterp() throws Exception{
            DrillConfig config = DrillConfig.create();
            final String jsonFile = "/constant2.json";
            LogicalPlan plan = LogicalPlan.parse(config, FileUtils.getResourceAsString(jsonFile));
            org.junit.Assert.assertEquals("Constant operator not read in properly or not recognized as a source operator.",
                    plan.getGraph().getLeaves().toString(), "[Constant [memo=null]]");

            org.junit.Assert.assertEquals("Edge between constant operator and sink not recognized.",
                    plan.getGraph().getRoots().toString(), "[Store [memo=output sink]]");

            
            IteratorRegistry ir = new IteratorRegistry();
            ReferenceInterpreter i = new ReferenceInterpreter(plan, ir, new BasicEvaluatorFactory(ir), new RSERegistry(config));
            i.setup();
            Collection<RunOutcome> outcomes = i.run();

            for(RunOutcome outcome : outcomes){
                System.out.println("============");
                System.out.println(outcome);
                if(outcome.outcome == RunOutcome.OutcomeType.FAILED && outcome.exception != null){
                    outcome.exception.printStackTrace();
                }
            }

    }
}

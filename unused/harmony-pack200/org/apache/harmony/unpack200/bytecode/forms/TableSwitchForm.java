/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.harmony.unpack200.bytecode.forms;

import org.apache.harmony.unpack200.bytecode.ByteCode;
import org.apache.harmony.unpack200.bytecode.OperandManager;

public class TableSwitchForm extends SwitchForm {

    public TableSwitchForm(int opcode, String name) {
        super(opcode, name);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.harmony.unpack200.bytecode.forms.SwitchForm#setByteCodeOperands(org.apache.harmony.unpack200.bytecode.ByteCode,
     *      org.apache.harmony.unpack200.bytecode.OperandManager, int)
     */
    public void setByteCodeOperands(ByteCode byteCode,
            OperandManager operandManager, int codeLength) {
        final int case_count = operandManager.nextCaseCount();
        final int default_pc = operandManager.nextLabel();
        int case_value = -1;
        case_value = operandManager.nextCaseValues();

        final int case_pcs[] = new int[case_count];
        for (int index = 0; index < case_count; index++) {
            case_pcs[index] = operandManager.nextLabel();
        }

        final int[] labelsArray = new int[case_count + 1];
        labelsArray[0] = default_pc;
        for (int index = 1; index < case_count + 1; index++) {
            labelsArray[index] = case_pcs[index - 1];
        }
        byteCode.setByteCodeTargets(labelsArray);

        final int lowValue = case_value;
        final int highValue = lowValue + case_count - 1;
        // All this gets dumped into the rewrite bytes of the
        // poor bytecode.

        // Unlike most byte codes, the TableSwitch is a
        // variable-sized bytecode. Because of this, the
        // rewrite array has to be defined here individually
        // for each bytecode, rather than in the ByteCodeForm
        // class.

        // First, there's the bytecode. Then there are 0-3
        // bytes of padding so that the first (default)
        // label is on a 4-byte offset.
        final int padLength = 3 - (codeLength % 4);
        final int rewriteSize = 1 + padLength + 4 // defaultbytes
                + 4 // lowbyte
                + 4 // highbyte
                + (4 * case_pcs.length);

        final int[] newRewrite = new int[rewriteSize];
        int rewriteIndex = 0;

        // Fill in what we can now
        // opcode
        newRewrite[rewriteIndex++] = byteCode.getOpcode();

        // padding
        for (int index = 0; index < padLength; index++) {
            newRewrite[rewriteIndex++] = 0;
        }

        // defaultbyte
        // This gets overwritten by fixUpByteCodeTargets
        newRewrite[rewriteIndex++] = -1;
        newRewrite[rewriteIndex++] = -1;
        newRewrite[rewriteIndex++] = -1;
        newRewrite[rewriteIndex++] = -1;

        // lowbyte
        final int lowbyteIndex = rewriteIndex;
        setRewrite4Bytes(lowValue, lowbyteIndex, newRewrite);
        rewriteIndex += 4;

        // highbyte
        final int highbyteIndex = rewriteIndex;
        setRewrite4Bytes(highValue, highbyteIndex, newRewrite);
        rewriteIndex += 4;

        // jump offsets
        // The case_pcs will get overwritten by fixUpByteCodeTargets
        for (int index = 0; index < case_count; index++) {
            // offset
            newRewrite[rewriteIndex++] = -1;
            newRewrite[rewriteIndex++] = -1;
            newRewrite[rewriteIndex++] = -1;
            newRewrite[rewriteIndex++] = -1;
        }
        byteCode.setRewrite(newRewrite);
    }
}

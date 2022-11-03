package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    private List<Instruction> originInstructions = new ArrayList<>();
    private final List<String> asmInstructions = new ArrayList<>();
    private final Map<Integer, IRVariable> regMap = new HashMap<>();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        this.originInstructions = originInstructions;
    }

    private int findReg(IRVariable reg) throws Exception {
        if (regMap.containsValue(reg)) {
            for (int i = 0; i <= 6; i++) {
                if (regMap.containsKey(i) && regMap.get(i).equals(reg)) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i <= 6; i++) {
                if (!regMap.containsKey(i)) {
                    regMap.put(i, reg);

                    return i;
                }
            }
        }

        throw new Exception("No Empty Reg");
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        for (var inst : originInstructions) {
            int regResult, regLHS, regRHS;
            switch(inst.getKind()) {
                case MOV -> {
                    if (inst.getFrom().isImmediate()) {
                        try {
                            regResult = findReg(inst.getResult());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("li t" + regResult + ", " + ((IRImmediate)inst.getFrom()).getValue());
                    } else {
                        try {
                            regLHS = findReg((IRVariable)inst.getFrom());
                            regResult = findReg(inst.getResult());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("mv t" + regResult + ", t" + regLHS);
                        if (((IRVariable) inst.getFrom()).isTemp()) {
                            regMap.remove(regLHS);
                        }
                    }
                }
                case ADD -> {
                    if (inst.getLHS().isImmediate() && inst.getRHS().isImmediate()) {
                        try {
                            regResult = findReg(inst.getResult());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        IRImmediate lhs = (IRImmediate)inst.getLHS(), rhs = (IRImmediate)inst.getRHS();
                        asmInstructions.add("li t" + regResult + ", " + (lhs.getValue() + rhs.getValue()));
                    } else if (inst.getLHS().isImmediate() || inst.getRHS().isImmediate()) {
                        int rhsValue;
                        IRVariable lhs;
                        if (inst.getLHS().isImmediate()) {
                            rhsValue = ((IRImmediate)inst.getLHS()).getValue();
                        } else {
                            rhsValue = ((IRImmediate)inst.getRHS()).getValue();
                        }
                        lhs = (IRVariable)inst.getRHS();

                        try {
                            regResult = findReg(inst.getResult());
                            regLHS = findReg(lhs);
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("addi t" + regResult + ", t" + regLHS + ", " + rhsValue);

                        if (lhs.isTemp()) {
                            regMap.remove(regLHS);
                        }
                    } else {
                        try {
                            regResult = findReg(inst.getResult());
                            regLHS = findReg((IRVariable) inst.getLHS());
                            regRHS = findReg((IRVariable) inst.getRHS());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("add t" + regResult + ", t" + regLHS + ", t" + regRHS);
                        if (((IRVariable) inst.getLHS()).isTemp()) {
                            regMap.remove(regLHS);
                        }
                        if (((IRVariable) inst.getRHS()).isTemp()) {
                            regMap.remove(regRHS);
                        }
                    }
                }
                case SUB -> {
                    if (inst.getLHS().isImmediate() && inst.getRHS().isImmediate()) {
                        try {
                            regResult = findReg(inst.getResult());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        IRImmediate lhs = (IRImmediate)inst.getLHS(), rhs = (IRImmediate)inst.getRHS();
                        asmInstructions.add("li t" + regResult + ", " + (lhs.getValue() - rhs.getValue()));
                    } else if (inst.getRHS().isImmediate()) {
                        try {
                            regResult = findReg(inst.getResult());
                            regLHS = findReg((IRVariable) inst.getLHS());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("subi t" + regResult + ", t" + regLHS + ", " + ((IRImmediate)inst.getRHS()).getValue());

                        if (((IRVariable) inst.getLHS()).isTemp()) {
                            regMap.remove(regLHS);
                        }
                    } else if (inst.getLHS().isImmediate()) {
                        int regTemp;
                        try {
                            regTemp = findReg(IRVariable.temp());
                            regResult = findReg(inst.getResult());
                            regRHS = findReg((IRVariable) inst.getRHS());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("li t" + regTemp + ", " + ((IRImmediate) inst.getLHS()).getValue());
                        asmInstructions.add("sub t" + regResult + ", t" + regTemp + ", t" + regRHS);

                        regMap.remove(regTemp);
                        if (((IRVariable) inst.getRHS()).isTemp()) {
                            regMap.remove(regRHS);
                        }
                    } else {
                        try {
                            regResult = findReg(inst.getResult());
                            regLHS = findReg((IRVariable) inst.getLHS());
                            regRHS = findReg((IRVariable) inst.getRHS());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("sub t" + regResult + ", t" + regLHS + ", t" + regRHS);
                        if (((IRVariable) inst.getLHS()).isTemp()) {
                            regMap.remove(regLHS);
                        }
                        if (((IRVariable) inst.getRHS()).isTemp()) {
                            regMap.remove(regRHS);
                        }
                    }
                }
                case MUL -> {
                    if (inst.getLHS().isImmediate() && inst.getRHS().isImmediate()) {
                        try {
                            regResult = findReg(inst.getResult());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        IRImmediate lhs = (IRImmediate)inst.getLHS(), rhs = (IRImmediate)inst.getRHS();
                        asmInstructions.add("li t" + regResult + ", " + (lhs.getValue() * rhs.getValue()));
                    } else if (inst.getLHS().isImmediate() || inst.getRHS().isImmediate()) {
                        IRVariable lhs;
                        IRImmediate rhs;
                        if (inst.getLHS().isImmediate()) {
                            lhs = (IRVariable) inst.getRHS();
                            rhs = (IRImmediate) inst.getLHS();
                        } else {
                            lhs = (IRVariable) inst.getLHS();
                            rhs = (IRImmediate) inst.getRHS();
                        }

                        int regTemp;
                        try {
                            regTemp = findReg(IRVariable.temp());
                            regResult = findReg(inst.getResult());
                            regLHS = findReg(lhs);
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("li t" + regTemp + ", " + rhs.getValue());
                        asmInstructions.add("mul t" + regResult + ", t" + regLHS + ", t" + regTemp);

                        regMap.remove(regTemp);

                        if (lhs.isTemp()) {
                            regMap.remove(regLHS);
                        }
                    } else {
                        try {
                            regResult = findReg(inst.getResult());
                            regLHS = findReg((IRVariable) inst.getLHS());
                            regRHS = findReg((IRVariable) inst.getRHS());
                        } catch(Exception e) {
                            e.printStackTrace();

                            return;
                        }

                        asmInstructions.add("mul t" + regResult + ", t" + regLHS + ", t" + regRHS);
                        if (((IRVariable) inst.getLHS()).isTemp()) {
                            regMap.remove(regLHS);
                        }
                        if (((IRVariable) inst.getRHS()).isTemp()) {
                            regMap.remove(regRHS);
                        }
                    }
                }
                case RET -> {
                    try {
                        regLHS = findReg((IRVariable) inst.getReturnValue());
                    } catch(Exception e) {
                        e.printStackTrace();

                        return;
                    }
                    asmInstructions.add("mv a0, t" + regLHS);
                }
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        FileUtils.writeLines(path, asmInstructions);
    }
}


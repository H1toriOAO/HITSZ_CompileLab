package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private SymbolTable symbolTable;
    private List<Instruction> IRList = new ArrayList<>();
    private Stack<IRValue> varStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        if (currentToken.getKindId().equals("id")) {
            if (symbolTable.has(currentToken.getText())) {
                varStack.push(IRVariable.named(currentToken.getText()));
            }
        } else if (currentToken.getKindId().equals("IntConst")) {
            varStack.push(IRImmediate.of(Integer.valueOf(currentToken.getText())));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 4 -> {
                varStack.pop();
            }
            case 6 -> {
                var arg1 = varStack.pop();
                IRVariable arg2 = (IRVariable) varStack.pop();
                IRList.add(Instruction.createMov(arg2, arg1));
            }
            case 7 -> {
                IRList.add(Instruction.createRet(varStack.pop()));
            }
            case 8 -> {
                var tmpVar = IRVariable.temp();
                var arg1 = varStack.pop();
                var arg2 = varStack.pop();
                IRList.add(Instruction.createAdd(tmpVar, arg2, arg1));
                varStack.push(tmpVar);
            }
            case 9 -> {
                var tmpVar = IRVariable.temp();
                var arg1 = varStack.pop();
                var arg2 = varStack.pop();
                IRList.add(Instruction.createSub(tmpVar, arg2, arg1));
                varStack.push(tmpVar);
            }
            case 11 -> {
                var tmpVar = IRVariable.temp();
                var arg1 = varStack.pop();
                var arg2 = varStack.pop();
                IRList.add(Instruction.createMul(tmpVar, arg2, arg1));
                varStack.push(tmpVar);
            }
            default -> {
                // do nothing
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
    }

    public List<Instruction> getIR() {
        return IRList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}


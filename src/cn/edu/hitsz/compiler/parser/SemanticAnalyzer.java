package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable;
    private Stack<SourceCodeType> typeStk = new Stack<>();
    private Stack<Token> tokenStk = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // do nothing
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 4 -> { // S -> D id
                var tokenTop = tokenStk.pop();

                if (symbolTable.has(tokenTop.getText())) {
                    var p = symbolTable.get(tokenTop.getText());
                    p.setType(typeStk.pop());
                }
            }
            case 5 -> { // D -> int
                typeStk.push(SourceCodeType.Int);
            }
            default -> {
                // do nothing
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        tokenStk.push(currentToken);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
    }
}


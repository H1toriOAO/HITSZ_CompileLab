package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();
    private final List<Token> tokenList = new ArrayList<>();
    private LRTable table;
    private Stack<Status> statusStack = new Stack<>();
    private Stack<Term> termStack = new Stack<>();

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        for (Token token : tokens) {
            tokenList.add(token);
        }
    }

    public void loadLRTable(LRTable table) {
        this.table = table;
    }

    public void run() {
        statusStack.push(table.getInit());
        termStack.push(TokenKind.eof());

        for (int i = 0; i < tokenList.size(); ) {
            Token token = tokenList.get(i);
            Action currentAction = table.getAction(statusStack.peek(), token);
            switch (currentAction.getKind()) {
                case Accept -> {
                    callWhenInAccept(statusStack.peek());
                    return;
                }
                case Error -> {
                    throw new RuntimeException("LR Error");
                }
                case Shift -> {
                    termStack.push(token.getKind());
                    statusStack.push(currentAction.getStatus());
                    i++;
                    callWhenInShift(currentAction.getStatus(), token);
                }
                case Reduce -> {
                    Production prod = currentAction.getProduction();
                    int size_p = prod.body().size();

                    for (int j = 0; j < size_p && !statusStack.empty() && !termStack.empty(); j++) {
                        statusStack.pop();
                        termStack.pop();
                    }

                    termStack.push(prod.head());
                    statusStack.push(table.getGoto(statusStack.peek(), prod.head()));
                    callWhenInReduce(statusStack.peek(), prod);
                }
            }
        }
    }
}

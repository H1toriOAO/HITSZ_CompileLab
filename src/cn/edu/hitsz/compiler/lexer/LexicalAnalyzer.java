package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String content;
    final private ArrayList<Token> tokenList = new ArrayList<>();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            StringBuilder stringBuilder = new StringBuilder();

            String currLine;
            while ((currLine = bufferedReader.readLine()) != null) {
                stringBuilder.append(currLine).append('\n');
            }

            content = stringBuilder.toString();

            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        var status = 0;
        String idName = "";
        var i = 0;
        while (i < content.length()) {
            char ch = content.charAt(i);

            // 自动机实现
            switch (status) {
                case 0 -> {
                    if (ch >= '0' && ch <= '9') {
                        status = 1;
                        idName += ch;
                    } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch == '_')) {
                        status = 2;
                        idName += ch;
                    } else if (ch == '=') {
                        status = 3;
                    } else if (ch == ',') {
                        status = 4;
                    } else if (ch == ';') {
                        status = 5;
                    } else if (ch == '+') {
                        status = 6;
                    } else if (ch == '-') {
                        status = 7;
                    } else if (ch == '*') {
                        status = 8;
                    } else if (ch == '/') {
                        status = 9;
                    } else if (ch == '(') {
                        status = 10;
                    } else if (ch == ')') {
                        status = 11;
                    } else {
                        i++;
                        continue;
                    }
                    i++;
                }
                case 1 -> {
                    if (ch >= '0' && ch <= '9') {
                        idName += ch;
                        i++;
                    } else {
                        final var token = Token.normal("IntConst", idName);
                        tokenList.add(token);
                        status = 0;
                        idName = "";
                    }
                }
                case 2 -> {
                    if ((ch >= 'a' && ch <= 'z')
                            || (ch >= 'A' && ch <= 'Z')
                            || (ch >= '0' && ch <= '9')) {
                        idName += ch;
                        i++;
                    } else {
                        if (idName.equals("int")) {
                            tokenList.add(Token.simple("int"));
                        } else if (idName.equals("return")) {
                            tokenList.add(Token.simple("return"));
                        } else {
                            tokenList.add(Token.normal("id", idName));
                            if (!symbolTable.has(idName)) {
                                symbolTable.add(idName);
                            }
                        }

                        status = 0;
                        idName = "";
                    }
                }
                case 3 -> {
                    status = 0;
                    tokenList.add(Token.simple("="));
                }
                case 4 -> {
                    status = 0;
                    tokenList.add(Token.simple(","));
                }
                case 5 -> {
                    status = 0;
                    tokenList.add(Token.simple("Semicolon"));
                }
                case 6 -> {
                    status = 0;
                    tokenList.add(Token.simple("+"));
                }
                case 7 -> {
                    status = 0;
                    tokenList.add(Token.simple("-"));
                }
                case 8 -> {
                    status = 0;
                    tokenList.add(Token.simple("*"));
                }
                case 9 -> {
                    status = 0;
                    tokenList.add(Token.simple("/"));
                }
                case 10 -> {
                    status = 0;
                    tokenList.add(Token.simple("("));
                }
                case 11 -> {
                    status = 0;
                    tokenList.add(Token.simple(")"));
                }
            }
        }
        tokenList.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}

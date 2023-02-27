/**
 * @Author: turk
 * @Description: Leksikalni analizator.
 */

package compiler.lexer;

import static common.RequireNonNull.requireNonNull;
import static compiler.lexer.TokenType.*;
import compiler.lexer.Position.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.Report;

public class Lexer {
    /**
     * Izvorna koda.
     */
    private final String source;

    /**
     * Preslikava iz ključnih besed v vrste simbolov.
     */
    private final static Map<String, TokenType> keywordMapping;

    static {
        keywordMapping = new HashMap<>();
        for (var token : TokenType.values()) {
            var str = token.toString();
            if (str.startsWith("KW_")) {
                keywordMapping.put(str.substring("KW_".length()).toLowerCase(), token);
            }
            if (str.startsWith("AT_")) {
                keywordMapping.put(str.substring("AT_".length()).toLowerCase(), token);
            }
        }
    }

    /**
     * Ustvari nov analizator.
     * 
     * @param source Izvorna koda programa.
     */
    public Lexer(String source) {
        requireNonNull(source);
        this.source = source;
    }

    /**
     * Izvedi leksikalno analizo.
     * 
     * @return seznam leksikalnih simbolov.
     */
    public List<Symbol> scan() {
        var symbols = new ArrayList<Symbol>();
        // todo: implementacija leksikalne analize
        
        var word = "";
        int startCol = 1;
        int startLine = 1;
        int wordLen = 0;
        boolean isComment = false;
        boolean isString = false;

        for(int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);

            if (isComment && !((int)c == 10 || (int)c == 13)) {
                startCol++;
                continue;
            } else if (isString) {
                if((int)c >= 32 && (int)c <= 126) {
                    if ((int)c == '\'') {
                        if(i < (source.length()-1) && (int)source.charAt(i+1) == '\'') {
                            word += '\'';
                            i++;
                            continue;
                        }
                        isString = false;
                        symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + wordLen)), C_STRING, word));
                        word = "";
                        continue;
                    }
                    startCol++;
                    word += c;
                    continue;
                } else {
                    Report.error(Position.fromLocation(new Location(startLine, startCol)), "String does not support this type of character");
                }
            }

            switch(c) {
                case (char)32:                // Space
                    startCol++;
                    word = "";
                    break;
                case (char)9:                 // Tab
                    startCol += 4;
                    word = "";
                    break;
                case (char)10:                // New line (we don't do \r here)
                    isComment = false;
                    startCol = 1;
                    startLine++;
                    word = "";
                    break;
                case '#':
                    isComment = true;
                    break;
                case '\'':
                    word = "";
                    isString = true;
                    break;
                case '$':
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), EOF, "$"));
                    break;
                case '+':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_ADD, "+"));
                    startCol++;
                    break;
                case '-':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_ADD, "-"));
                    startCol++;
                    break;
                case '*':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_MUL, "*"));
                    startCol++;
                    break;
                case '/':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_DIV, "/"));
                    startCol++;
                    break;
                case '%':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_MOD, "%"));
                    startCol++;
                    break;
                case '&':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_AND, "&"));
                    startCol++;
                    break;
                case '|':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_OR, "|"));
                    startCol++;
                    break;
                case '!':
                    word = "";
                    if (source.charAt(i + 1) == '=') {
                        word = "!";
                        wordLen++;
                        continue;
                    }
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_NOT, "!"));
                    startCol++;
                    break;
                case '<':
                    word = "";
                    if (source.charAt(i + 1) == '=') {
                        word = "<";
                        wordLen++;
                        continue;
                    }
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_LT, "<"));
                    startCol++;
                    break;
                case '>':
                    word = "";
                    if (source.charAt(i + 1) == '=') {
                        word = ">";
                        wordLen++;
                        continue;
                    }
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_GT, ">"));
                    startCol++;
                    break;
                case '(':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_LPARENT, "("));
                    startCol++;
                    break;
                case ')':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_RPARENT, ")"));
                    startCol++;
                    break;
                case '{':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_LBRACE, "{"));
                    startCol++;
                    break;
                case '}':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_RBRACE, "}"));
                    startCol++;
                    break;
                case '[':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_LBRACKET, "["));
                    startCol++;
                    break;
                case ']':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_RBRACKET, "]"));
                    startCol++;
                    break;
                case ':':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_COLON, ":"));
                    startCol++;
                    break;
                case ';':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_SEMICOLON, ";"));
                    startCol++;
                    break;
                case '.':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_DOT, "."));
                    startCol++;
                    break;
                case ',':
                    word = "";
                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_COMMA, ","));
                    startCol++;
                    break;
                case '=':
                    if (word.length() == 1) {
                        if (word == "!") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + wordLen)), OP_NEQ, "!="));
                        } else if (word == "=") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + wordLen)), OP_EQ, "=="));
                        } else if (word == "<") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + wordLen)), OP_LEQ, "<="));
                        } else if (word == ">") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + wordLen)), OP_GEQ, ">="));
                        }
                        word = "";
                        continue;
                    }
                    if (source.charAt(i + 1) == '=') {
                        word = "=";
                        continue;
                    }

                    symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), OP_ASSIGN, "="));

                    break;
                default:
                    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
                    break;
            }
        }


        return symbols;
    }
}

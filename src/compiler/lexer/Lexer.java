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
        
        char c;
        char next;
        var word = "";
        int startCol = 1;
        int startLine = 1;
        int stringLen = 0;
        boolean isComment = false;
        boolean isString = false;
        boolean isNum = false;
        boolean isTokenWord = false;

        for(int i = 0; i < source.length(); i++) {
            c = source.charAt(i);

            // Comments
            if (isComment && !((int)c == 10 || (int)c == 13)) {
                startCol++;
                continue;
            }

            // Strings
            else if (isString) {
                if((int)c >= 32 && (int)c <= 126) {
                    // handle ' in string
                    if ((int)c == '\'') {
                        // if there are two ' in a row
                        if(i < (source.length()-1) && (int)source.charAt(i+1) == '\'') {
                            stringLen += 2;
                            word += '\'';
                            i++;
                            continue;
                        }
                        // ending string
                        isString = false;
                        stringLen++;
                        symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + stringLen)), C_STRING, word));
                        startCol += stringLen;
                        word = "";
                        continue;
                    }

                    // normal string character
                    word += c;
                    stringLen++;
                    continue;
                } else {
                    // if wrong character
                    Report.error(Position.fromLocation(new Location(startLine, startCol + word.length() + 1)), "String does not support this type of character");
                }
            }

            // Numbers
            else if (c >= '0' && c <= '9' && !isTokenWord) {
                if (isNum) {

                    // When there is no next integer or when the next char is the end of file
                    if (i < (source.length() - 1) && !((char)source.charAt(i + 1) >= '0' && (char)source.charAt(i + 1) <= '9') || i == source.length() - 1) {
                        word += c;
                        symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + word.length())), C_INTEGER, word));
                        isNum = false;
                        startCol += word.length();
                        word = "";
                        continue;
                    }

                    // Normal integer
                    word += c;
                    continue;
                }

                // Start number
                if (word == "") {
                    isNum = true;
                    word = "" + c;
                    continue;
                }
            }

            // Words 
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                if (word == "") {
                    isTokenWord = true;
                }
                word += c;

                next = (i < source.length() - 1) ? source.charAt(i + 1) : '$';
                if ((!(next >= 'a' && next <= 'z') && !(next >= 'A' && next <= 'Z') && !(next >= '0' && next <= '9') && next != '_')) {
                    if (word.equals("true") || word.equals("false"))
                        symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + word.length())), C_LOGICAL, word));
                    else if (keywordMapping.containsKey(word))
                        symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + word.length())), keywordMapping.get(word), word));
                    else
                        symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + word.length())), IDENTIFIER, word));

                    startCol += word.length();
                    word = "";
                    isTokenWord = false;
                    // check which word it is 
                }
            }

            // 1 and 2 char symbols + string and comment start
            switch(c) {
                case ' ':
                    startCol++;
                    word = "";
                    break;
                case '\t':
                    startCol += 4;
                    word = "";
                    break;
                case '\n':              // (we don't do \r here)
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
                    stringLen = 1;
                    isString = true;
                    break;
                case '$':
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), EOF, "$"));
                    break;
                case '+':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_ADD, "+"));
                    break;
                case '-':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_SUB, "-"));
                    break;
                case '*':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_MUL, "*"));
                    break;
                case '/':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_DIV, "/"));
                    break;
                case '%':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_MOD, "%"));
                    break;
                case '&':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_AND, "&"));
                    break;
                case '|':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_OR, "|"));
                    break;
                case '!':
                    word = "";
                    if (i < (source.length() - 1) && source.charAt(i + 1) == '=') {
                        word = "!";
                        continue;
                    }
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_NOT, "!"));
                    break;
                case '<':
                    word = "";
                    if (i < (source.length()-1) && source.charAt(i + 1) == '=') {
                        word = "<";
                        continue;
                    }
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_LT, "<"));
                    break;
                case '>':
                    word = "";
                    if (i < (source.length()-1) && source.charAt(i + 1) == '=') {
                        word = ">";
                        continue;
                    }
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_GT, ">"));
                    break;
                case '(':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_LPARENT, "("));
                    break;
                case ')':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_RPARENT, ")"));
                    break;
                case '{':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_LBRACE, "{"));
                    break;
                case '}':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_RBRACE, "}"));
                    break;
                case '[':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_LBRACKET, "["));
                    break;
                case ']':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_RBRACKET, "]"));
                    break;
                case ':':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_COLON, ":"));
                    break;
                case ';':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_SEMICOLON, ";"));
                    break;
                case '.':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_DOT, "."));
                    break;
                case ',':
                    word = "";
                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_COMMA, ","));
                    break;
                case '=':
                    if (word.length() == 1) {
                        if (word == "!") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + 2)), OP_NEQ, "!="));
                        } else if (word == "=") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + 2)), OP_EQ, "=="));
                        } else if (word == "<") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + 2)), OP_LEQ, "<="));
                        } else if (word == ">") {
                            symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, startCol + 2)), OP_GEQ, ">="));
                        }
                        startCol += 2;
                        word = "";
                        continue;
                    }
                    if (i < (source.length()-1) && source.charAt(i + 1) == '=') {
                        word = "=";
                        continue;
                    }

                    symbols.add(new Symbol(new Position(new Location(startLine, startCol), new Location(startLine, ++startCol)), OP_ASSIGN, "="));

                    break;
            }
        }
        if (isString)
            Report.error("String not closed!!"); 
        symbols.add(new Symbol(Position.fromLocation(new Location(startLine, startCol)), EOF, "$"));
        

        return symbols;
    }
}
